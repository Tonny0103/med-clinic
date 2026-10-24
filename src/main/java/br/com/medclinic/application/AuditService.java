package br.com.medclinic.application;

import static br.com.medclinic.domain.Rules.require;

import br.com.medclinic.domain.Patient;
import br.com.medclinic.domain.RuleViolation;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AuditService {
    private final PatientService patients;

    public AuditService(PatientService patients) {
        this.patients = patients;
    }

    public record PatientOption(UUID id, String label) {}

    public record Report(Page<ClinicStore.Event> page, List<PatientOption> patients) {}

    public Report query(Actor actor, Map<String, String> filters, int page) {
        patients.authorize(actor, actor.isManager(), "CONSULTAR_AUDITORIA", null);
        try {
            String actorFilter = patients.value(filters, "actorFilter");
            String operation = patients.value(filters, "operation").toUpperCase(java.util.Locale.ROOT);
            String patient = patients.value(filters, "patient");
            require(
                    "filters",
                    actorFilter.length() <= 100 && operation.length() <= 80 && patient.length() <= 36,
                    "Filtro muito longo.");
            LocalDate from = optionalDate(filters, "from"), to = optionalDate(filters, "to");
            require(
                    "to",
                    from == null || to == null || !to.isBefore(from),
                    "O fim do período não pode anteceder o início.");
            Instant start = from == null
                    ? null
                    : from.atStartOfDay(patients.clock.getZone()).toInstant();
            Instant end = to == null
                    ? null
                    : to.plusDays(1).atStartOfDay(patients.clock.getZone()).toInstant();
            return patients.store.transaction(() -> {
                List<Patient> all = patients.store.all();
                Set<UUID> identities = new java.util.HashSet<>();
                if (!patient.isBlank()) {
                    Patient selected = all.stream()
                            .filter(p -> p.id.toString().equals(patient) || p.chart.equals(patient))
                            .findFirst()
                            .orElseThrow(() -> new RuleViolation("patient", "Paciente do filtro não encontrado."));
                    identities.add(selected.id);
                    if (selected.mergedInto == null)
                        all.stream()
                                .filter(p -> patients.canonical(p.id).id.equals(selected.id))
                                .forEach(p -> identities.add(p.id));
                }
                var filtered = patients.store.events().stream()
                        .filter(event -> actorFilter.isBlank() || event.actor().equals(actorFilter))
                        .filter(event ->
                                operation.isBlank() || event.operation().equals(operation))
                        .filter(event -> patient.isBlank() || identities.contains(event.patientId()))
                        .filter(event -> start == null || !event.at().isBefore(start))
                        .filter(event -> end == null || event.at().isBefore(end))
                        .toList();
                patients.event(actor, "CONSULTAR_AUDITORIA", null, "SUCESSO", "filters", "", "");
                List<PatientOption> options = all.stream()
                        .map(p -> new PatientOption(p.id, p.name + " · " + p.chart + " · " + p.status))
                        .toList();
                return new Report(Page.of(filtered, page), options);
            });
        } catch (RuleViolation error) {
            patients.event(actor, "CONSULTAR_AUDITORIA", null, "RECUSADO", "", "", "");
            throw error;
        }
    }

    private LocalDate optionalDate(Map<String, String> filters, String key) {
        return patients.value(filters, key).isBlank() ? null : patients.date(filters, key);
    }
}
