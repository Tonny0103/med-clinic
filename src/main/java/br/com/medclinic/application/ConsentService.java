package br.com.medclinic.application;

import static br.com.medclinic.domain.Rules.*;

import br.com.medclinic.domain.Patient;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ConsentService {
    public static final java.util.List<String> PURPOSES =
            java.util.List.of("PROTECAO_DADOS", "TERMOS_ATENDIMENTO", "IMAGEM");
    private final PatientService patients;
    private final AttachmentService attachments;

    public ConsentService(PatientService patients, AttachmentService attachments) {
        this.patients = patients;
        this.attachments = attachments;
    }

    public UUID record(
            Actor actor,
            UUID id,
            long version,
            UUID token,
            Map<String, String> form,
            boolean revocation,
            byte[] content,
            String mediaType) {
        String operation = revocation ? "REVOGAR" : "CONSENTIMENTO";
        patients.requireWriter(actor, operation, id);
        boolean newFile = content != null;
        Map<String, String> payload = new java.util.TreeMap<>(newFile ? attachments.payload(form, content) : form);
        payload.put("decision", revocation ? "REVOGADO" : patients.value(form, "decision"));
        payload.put("newEvidence", Boolean.toString(newFile));
        payload.put("mediaType", mediaType == null ? "" : mediaType);
        if (patients.store.receipt(actor.username(), token).isPresent())
            return patients.write(actor, id, version, token, operation, payload, p -> {
                throw new IllegalStateException();
            });
        PrivateFiles.Prepared prepared;
        try {
            require(
                    "evidenceId",
                    newFile != !patients.value(form, "evidenceId").isBlank(),
                    "Escolha exatamente uma prova existente ou um novo arquivo.");
            require("file", revocation || !newFile, "Nova evidência neste formulário só é permitida para revogação.");
            patients.store.transaction(() -> {
                Patient current = patients.current(id, version);
                if (!revocation) patients.active(current);
                return null;
            });
            prepared = newFile ? attachments.files.prepare(content, mediaType) : null;
        } catch (br.com.medclinic.domain.RuleViolation error) {
            patients.event(actor, operation, id, "RECUSADO", "", "", "");
            throw error;
        }
        return patients.write(actor, id, version, token, operation, payload, p -> {
            if (!revocation) patients.active(p);
            String purpose = patients.value(form, "purpose"), decision = payload.get("decision");
            require("purpose", PURPOSES.contains(purpose), "Selecione uma finalidade válida.");
            require(
                    "decision",
                    revocation || Set.of("CONCEDIDO", "RECUSADO").contains(decision),
                    "Selecione aceite ou recusa. Revogação usa o fluxo próprio.");
            text("termVersion", patients.value(form, "termVersion"), 80);
            text("declarant", patients.value(form, "declarant"), 160);
            LocalDate manifestedOn = patients.date(form, "manifestedOn");
            require(
                    "manifestedOn",
                    !manifestedOn.isAfter(patients.today()) && !manifestedOn.isBefore(p.birthdate),
                    "Confira a data da manifestação.");
            UUID guardianId = patients.value(form, "guardianId").isBlank() ? null : patients.uuid(form, "guardianId");
            require(
                    "guardianId",
                    !p.birthdate.plusYears(18).isAfter(manifestedOn) || guardianId != null,
                    "A manifestação de menor exige responsável legal.");
            if (guardianId != null)
                require(
                        "guardianId",
                        p.guardians.stream().anyMatch(g -> g.id().equals(guardianId) && g.currentOn(manifestedOn)),
                        "Selecione responsável legal vigente na data da manifestação.");
            UUID evidenceId;
            if (prepared != null) {
                var attachment = attachments.attachment(p, prepared, "EVIDENCIA_REVOGACAO", actor);
                p.attachments.add(attachment);
                evidenceId = attachment.id();
                patients.event(
                        actor, "ANEXAR_EVIDENCIA_REVOGACAO", p.id, "SUCESSO", "attachment", "", evidenceId.toString());
            } else
                evidenceId =
                        patients.proof(p, patients.uuid(form, "evidenceId")).id();
            payload.put("evidenceId", evidenceId.toString());
            p.consents.add(new Patient.Consent(
                    UUID.randomUUID(),
                    p.id,
                    purpose,
                    patients.value(form, "termVersion"),
                    decision,
                    manifestedOn,
                    patients.clock.instant(),
                    patients.value(form, "declarant"),
                    guardianId,
                    actor.username(),
                    evidenceId,
                    p.version + 1));
        });
    }

    public static String status(Patient p, String purpose) {
        var decisions =
                p.consents.stream().filter(c -> c.purpose().equals(purpose)).toList();
        if (decisions.isEmpty()) return "NAO_INFORMADO";
        long lastMerge = p.merges.stream()
                .filter(m -> p.id.equals(m.destinationId()))
                .mapToLong(Patient.Merge::destinationVersion)
                .max()
                .orElse(0);
        var newer = decisions.stream()
                .filter(c -> c.origin().equals(p.id) && c.generation() > lastMerge)
                .max(Comparator.comparingLong(Patient.Consent::generation));
        if (newer.isPresent()) return effectiveDecision(p, newer.get());
        Map<UUID, Patient.Consent> latest = new HashMap<>();
        decisions.forEach(c -> latest.merge(c.origin(), c, (a, b) -> a.generation() >= b.generation() ? a : b));
        if (latest.values().stream().anyMatch(c -> c.decision().equals("REVOGADO"))) return "REVOGADO";
        if (latest.values().stream().anyMatch(c -> c.decision().equals("RECUSADO"))) return "RECUSADO";
        Set<UUID> origins = new java.util.HashSet<>();
        origins.add(p.id);
        p.merges.forEach(m -> origins.add(m.sourceId()));
        return latest.keySet().containsAll(origins)
                        && latest.values().stream()
                                        .map(Patient.Consent::termVersion)
                                        .distinct()
                                        .count()
                                == 1
                        && latest.values().stream()
                                .allMatch(c -> effectiveDecision(p, c).equals("CONCEDIDO"))
                ? "CONCEDIDO"
                : "SEM_AUTORIZACAO";
    }

    private static String effectiveDecision(Patient p, Patient.Consent decision) {
        if (!decision.decision().equals("CONCEDIDO")) return decision.decision();
        return p.attachments.stream().anyMatch(a -> a.id().equals(decision.evidenceId()) && a.administrative())
                ? "CONCEDIDO"
                : "SEM_AUTORIZACAO";
    }

    public static Map<String, String> statuses(Patient p) {
        Map<String, String> statuses = new java.util.LinkedHashMap<>();
        PURPOSES.forEach(purpose -> statuses.put(purpose, status(p, purpose)));
        return statuses;
    }
}
