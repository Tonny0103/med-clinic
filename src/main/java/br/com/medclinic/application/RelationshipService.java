package br.com.medclinic.application;

import static br.com.medclinic.domain.Rules.*;

import br.com.medclinic.domain.Patient;
import br.com.medclinic.domain.RuleViolation;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class RelationshipService {
    private final PatientService patients;

    public RelationshipService(PatientService patients) {
        this.patients = patients;
    }

    public UUID update(Actor actor, UUID id, long version, UUID token, String action, Map<String, String> form) {
        require(
                "action",
                java.util.Set.of(
                                "responsavel",
                                "encerrar-responsavel",
                                "emergencia",
                                "convenio",
                                "encerrar-convenio",
                                "preferencia")
                        .contains(action),
                "Operação desconhecida.");
        return patients.write(
                actor, id, version, token, "VINCULO_" + action.toUpperCase(java.util.Locale.ROOT), form, p -> {
                    patients.active(p);
                    switch (action) {
                        case "responsavel" -> addGuardian(p, form);
                        case "encerrar-responsavel" ->
                            endGuardian(p, patients.uuid(form, "guardianId"), patients.date(form, "guardianEndsOn"));
                        case "emergencia" -> emergency(p, form);
                        case "convenio" -> addCoverage(p, form);
                        case "encerrar-convenio" -> endCoverage(p, form);
                        case "preferencia" -> selectPreference(p, patients.value(form, "preference"));
                        default -> throw new IllegalStateException();
                    }
                    validate(p, patients.today());
                });
    }

    private void addGuardian(Patient p, Map<String, String> f) {
        LocalDate startsOn = patients.date(f, "guardianStartsOn");
        require("guardianStartsOn", !startsOn.isAfter(patients.today()), "O responsável deve iniciar até hoje.");
        if (!patients.value(f, "replaceGuardianId").isBlank()) {
            endGuardian(p, patients.uuid(f, "replaceGuardianId"), startsOn);
        }
        p.guardians.add(new Patient.Guardian(
                UUID.randomUUID(),
                p.id,
                patients.value(f, "guardianName"),
                patients.value(f, "guardianRelationship"),
                patients.value(f, "guardianDocument"),
                patients.value(f, "guardianPhone"),
                startsOn,
                null));
    }

    private void endGuardian(Patient p, UUID id, LocalDate endsOn) {
        var guardian = p.guardians.stream()
                .filter(g -> g.id().equals(id) && g.endsOn() == null)
                .findFirst()
                .orElseThrow(() -> new RuleViolation("guardianId", "Responsável atual não encontrado."));
        require(
                "guardianEndsOn",
                !endsOn.isBefore(guardian.startsOn()) && !endsOn.isAfter(patients.today()),
                "Confira a data de encerramento do responsável.");
        int index = p.guardians.indexOf(guardian);
        p.guardians.set(
                index,
                new Patient.Guardian(
                        guardian.id(),
                        guardian.origin(),
                        guardian.name(),
                        guardian.relationship(),
                        guardian.document(),
                        guardian.phone(),
                        guardian.startsOn(),
                        endsOn));
    }

    private void emergency(Patient p, Map<String, String> f) {
        String name = patients.value(f, "emergencyName");
        String relation = patients.value(f, "emergencyRelationship");
        String phone = patients.value(f, "emergencyPhone");
        if (name.isBlank() && relation.isBlank() && phone.isBlank()) {
            p.emergency = null;
            return;
        }
        text("emergencyName", name, 160);
        text("emergencyRelationship", relation, 80);
        phone("emergencyPhone", phone);
        p.emergency = new Patient.Emergency(name, relation, phone);
    }

    private void addCoverage(Patient p, Map<String, String> f) {
        String insurer = patients.value(f, "insurer"),
                plan = patients.value(f, "plan"),
                card = patients.value(f, "card");
        text("insurer", insurer, 160);
        text("plan", plan, 160);
        text("card", card, 100);
        LocalDate startsOn = patients.date(f, "startsOn"), expiresOn = patients.date(f, "expiresOn");
        UUID predecessor =
                patients.value(f, "supersedesCoverageId").isBlank() ? null : patients.uuid(f, "supersedesCoverageId");
        Patient.Coverage previous = null;
        if (predecessor != null) {
            previous = coverage(p, predecessor);
            require("supersedesCoverageId", previous.endsOn() == null, "O segmento anterior já está encerrado.");
            close(p, previous, startsOn);
        }
        Patient.Coverage next = new Patient.Coverage(
                UUID.randomUUID(), p.id, insurer, plan, card, expiresOn, startsOn, null, predecessor);
        p.coverages.add(next);
        if (previous != null && p.preference.equals(previous.id().toString())) {
            String choice = patients.value(f, "preferenceAfter");
            selectPreference(p, choice.equals("SUCCESSOR") ? next.id().toString() : choice);
        }
    }

    private void endCoverage(Patient p, Map<String, String> f) {
        Patient.Coverage previous = coverage(p, patients.uuid(f, "coverageId"));
        require("coverageId", previous.endsOn() == null, "O convênio já está encerrado.");
        close(p, previous, patients.date(f, "endsOn"));
        if (p.preference.equals(previous.id().toString())) selectPreference(p, patients.value(f, "preferenceAfter"));
    }

    private void close(Patient p, Patient.Coverage previous, LocalDate endsOn) {
        require("endsOn", !endsOn.isBefore(previous.startsOn()), "Encerramento não pode anteceder o início.");
        p.coverages.set(
                p.coverages.indexOf(previous),
                new Patient.Coverage(
                        previous.id(),
                        previous.origin(),
                        previous.insurer(),
                        previous.plan(),
                        previous.card(),
                        previous.expiresOn(),
                        previous.startsOn(),
                        endsOn,
                        previous.supersedesCoverageId()));
    }

    private Patient.Coverage coverage(Patient p, UUID id) {
        return p.coverages.stream()
                .filter(c -> c.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuleViolation("coverageId", "Convênio não encontrado nesta ficha."));
    }

    private void selectPreference(Patient p, String choice) {
        require("preference", !choice.isBlank(), "Escolha explicitamente particular ou um convênio atual.");
        if (!choice.equals("PARTICULAR")) {
            UUID id;
            try {
                id = UUID.fromString(choice);
            } catch (IllegalArgumentException invalid) {
                throw new RuleViolation("preference", "Preferência inválida.");
            }
            require("preference", coverage(p, id).inForceOn(patients.today()), "Escolha um convênio vigente hoje.");
        }
        p.preference = choice;
    }

    public static String effectivePreference(Patient p, LocalDate date) {
        return p.coverages.stream().anyMatch(c -> c.id().toString().equals(p.preference) && c.inForceOn(date))
                ? p.preference
                : "PARTICULAR";
    }
}
