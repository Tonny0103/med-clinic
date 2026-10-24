package br.com.medclinic.application;

import static br.com.medclinic.domain.Rules.require;

import br.com.medclinic.domain.Patient;
import br.com.medclinic.domain.RuleViolation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;

public class PatientService {
    protected final ClinicStore store;
    protected final Clock clock;

    public PatientService(ClinicStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public UUID create(Actor actor, UUID token, Map<String, String> form) {
        authorize(actor, actor.canWrite(), "CRIAR", null);
        String fingerprint = fingerprint(form);
        try {
            return store.transaction(() -> {
                var previous = store.receipt(actor.username(), token);
                if (previous.isPresent()) {
                    var receipt = previous.get();
                    require(
                            "token",
                            receipt.operation().equals("CRIAR")
                                    && receipt.fingerprint().equals(fingerprint),
                            "Submissão reutilizada com dados diferentes.");
                    return receipt.patientId();
                }
                Patient p = new Patient();
                applyProfile(p, form);
                if (!value(form, "guardianName").isBlank()
                        || !value(form, "guardianDocument").isBlank()
                        || !value(form, "guardianPhone").isBlank()) {
                    p.guardians.add(new Patient.Guardian(
                            UUID.randomUUID(),
                            p.id,
                            value(form, "guardianName"),
                            value(form, "guardianRelationship"),
                            value(form, "guardianDocument"),
                            value(form, "guardianPhone"),
                            today(),
                            null));
                }
                br.com.medclinic.domain.Rules.validate(p, today());
                checkSuspicion(p, value(form, "reason"));
                p.chart = store.nextChart();
                store.save(p, -1);
                event(actor, "CRIAR", p.id, "SUCESSO", auditFields(form), value(form, "reason"), "");
                store.receipt(actor.username(), token, new ClinicStore.Receipt("CRIAR", fingerprint, p.id));
                return p.id;
            });
        } catch (RuleViolation exception) {
            event(actor, "CRIAR", null, "RECUSADO", "", "", "");
            throw exception;
        }
    }

    public void requireWriter(Actor actor, String operation, UUID id) {
        authorize(actor, actor.canWrite(), operation, id);
    }

    public PatientView read(Actor actor, UUID id) {
        authorize(actor, actor.canRead(), "CONSULTAR", id);
        return store.transaction(() -> {
            Patient requested =
                    store.find(id).orElseThrow(() -> new RuleViolation("patient", "Paciente não encontrado."));
            Patient p = canonical(id);
            event(actor, "CONSULTAR", p.id, "SUCESSO", "", "", id.equals(p.id) ? "" : id.toString());
            return new PatientView(p, id.equals(p.id) ? "" : requested.chart);
        });
    }

    public record PatientView(Patient patient, String originalChart) {}

    protected void applyProfile(Patient p, Map<String, String> f) {
        p.name = value(f, "name");
        p.birthdate = date(f, "birthdate");
        p.sex = value(f, "sex");
        p.nationality = value(f, "nationality").toUpperCase(java.util.Locale.ROOT);
        p.cpf = value(f, "cpf");
        p.alternativeType = value(f, "alternativeType");
        p.alternativeNumber = value(f, "alternativeNumber");
        p.alternativeCountry = value(f, "alternativeCountry");
        p.alternativeIssuer = value(f, "alternativeIssuer");
        p.alternativeReason = value(f, "alternativeReason");
        p.phone = value(f, "phone");
        p.email = value(f, "email");
        p.address = value(f, "address");
        p.country = value(f, "country");
    }

    protected void checkSuspicion(Patient p, String reason) {
        var suspects = store.all().stream()
                .filter(other -> !other.id.equals(p.id) && other.mergedInto == null)
                .filter(other ->
                        br.com.medclinic.domain.Rules.suspicious(p.name, p.birthdate, other.name, other.birthdate))
                .limit(20)
                .map(other -> other.name + " · " + other.birthdate + " · prontuário " + other.chart)
                .toList();
        require(
                "reason",
                suspects.isEmpty() || !reason.isBlank(),
                "Revise possível duplicidade: " + String.join("; ", suspects)
                        + ". Documentos distintos permitem continuar com justificativa.");
        require("reason", reason.length() <= 500, "A justificativa deve ter até 500 caracteres.");
    }

    protected void authorize(Actor actor, boolean permitted, String operation, UUID patientId) {
        if (!permitted) {
            event(actor, operation, patientId, "NEGADO", "", "", "");
            throw new AccessDenied();
        }
    }

    protected void event(
            Actor actor,
            String operation,
            UUID patientId,
            String outcome,
            String fields,
            String reason,
            String references) {
        store.audit(new ClinicStore.Event(
                UUID.randomUUID(),
                clock.instant(),
                actor.username(),
                actor.role(),
                operation,
                patientId,
                outcome,
                fields,
                safeReason(reason),
                references));
    }

    private String safeReason(String value) {
        return value == null
                ? ""
                : value.replaceAll("[^\\s@]+@[^\\s@]+", "[omitido]").replaceAll("(?:\\d[ .()/-]*){7,}", "[omitido]");
    }

    protected Patient canonical(UUID id) {
        Patient p = store.find(id).orElseThrow(() -> new RuleViolation("patient", "Paciente não encontrado."));
        int hops = 0;
        while (p.mergedInto != null) {
            require("patient", ++hops <= 100, "Referência de paciente inválida.");
            p = store.find(p.mergedInto).orElseThrow(() -> new RuleViolation("patient", "Destino indisponível."));
        }
        return p;
    }

    protected Patient current(UUID id, long version) {
        Patient p = canonical(id);
        require("version", p.id.equals(id), "Cadastro mesclado. Abra a ficha de destino.");
        require("version", p.version == version, "A ficha foi alterada. Reabra e revise antes de salvar.");
        return p;
    }

    protected void active(Patient p) {
        require("status", "ATIVO".equals(p.status), "Esta alteração exige um paciente ativo.");
    }

    protected UUID write(
            Actor actor,
            UUID id,
            long version,
            UUID token,
            String operation,
            Map<String, String> form,
            Consumer<Patient> mutation) {
        authorize(actor, actor.canWrite(), operation, id);
        Map<String, String> payload = new TreeMap<>(form);
        payload.put("$patientId", id.toString());
        payload.put("$version", Long.toString(version));
        String fingerprint = fingerprint(payload);
        try {
            return store.transaction(() -> {
                var previous = store.receipt(actor.username(), token);
                if (previous.isPresent()) {
                    var receipt = previous.get();
                    require(
                            "token",
                            receipt.operation().equals(operation)
                                    && receipt.fingerprint().equals(fingerprint),
                            "Submissão reutilizada com dados diferentes.");
                    return receipt.patientId();
                }
                store.lock(java.util.List.of(id));
                Patient p = current(id, version);
                mutation.accept(p);
                store.save(p, version);
                event(
                        actor,
                        operation,
                        p.id,
                        "SUCESSO",
                        auditFields(form),
                        form.getOrDefault("reason", ""),
                        form.getOrDefault("evidenceId", ""));
                store.receipt(actor.username(), token, new ClinicStore.Receipt(operation, fingerprint, p.id));
                return p.id;
            });
        } catch (RuleViolation exception) {
            event(actor, operation, id, "RECUSADO", "", "", "");
            throw exception;
        }
    }

    protected String auditFields(Map<String, String> form) {
        return String.join(
                ",",
                form.keySet().stream()
                        .filter(k -> !java.util.Set.of("_csrf", "token", "version", "reason")
                                .contains(k))
                        .filter(k -> k.matches("[A-Za-z][A-Za-z0-9]{0,40}"))
                        .sorted()
                        .limit(40)
                        .toList());
    }

    protected String fingerprint(Map<String, String> form) {
        try {
            Map<String, String> clean = new TreeMap<>(form);
            clean.remove("_csrf");
            StringBuilder canonical = new StringBuilder();
            clean.forEach((key, value) -> canonical
                    .append(key.length())
                    .append(':')
                    .append(key)
                    .append(value.length())
                    .append(':')
                    .append(value));
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256")
                            .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    protected String value(Map<String, String> form, String key) {
        return form.getOrDefault(key, "").strip();
    }

    protected LocalDate date(Map<String, String> form, String key) {
        try {
            return LocalDate.parse(value(form, key));
        } catch (java.time.DateTimeException exception) {
            throw new RuleViolation(key, "Informe uma data válida.");
        }
    }

    protected UUID uuid(Map<String, String> form, String key) {
        try {
            return UUID.fromString(value(form, key));
        } catch (IllegalArgumentException exception) {
            throw new RuleViolation(key, "Selecione uma referência válida.");
        }
    }
}
