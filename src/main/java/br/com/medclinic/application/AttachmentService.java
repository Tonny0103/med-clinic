package br.com.medclinic.application;

import static br.com.medclinic.domain.Rules.*;

import br.com.medclinic.domain.Patient;
import br.com.medclinic.domain.RuleViolation;
import java.util.Map;
import java.util.UUID;

public class AttachmentService {
    final PatientService patients;
    final PrivateFiles files;

    public AttachmentService(PatientService patients, PrivateFiles files) {
        this.patients = patients;
        this.files = files;
    }

    Map<String, String> payload(Map<String, String> form, byte[] bytes) {
        var payload = new java.util.TreeMap<>(form);
        try {
            payload.put(
                    "fileDigest",
                    java.util.HexFormat.of()
                            .formatHex(java.security.MessageDigest.getInstance("SHA-256")
                                    .digest(bytes)));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
        return payload;
    }

    public UUID upload(
            Actor actor,
            UUID id,
            long version,
            UUID token,
            Map<String, String> form,
            byte[] content,
            String mediaType) {
        patients.requireWriter(actor, "ANEXAR", id);
        Map<String, String> payload = payload(form, content);
        PrivateFiles.Prepared prepared;
        try {
            patients.store.transaction(() -> {
                patients.active(patients.current(id, version));
                return null;
            });
            prepared = files.prepare(content, mediaType);
        } catch (RuleViolation error) {
            patients.event(actor, "ANEXAR", id, "RECUSADO", "", "", "");
            throw error;
        }
        return patients.write(actor, id, version, token, "ANEXAR", payload, p -> {
            patients.active(p);
            p.attachments.add(attachment(p, prepared, patients.value(form, "category"), actor));
        });
    }

    Patient.Attachment attachment(Patient patient, PrivateFiles.Prepared file, String category, Actor actor) {
        String safeCategory =
                java.util.Set.of("IDENTIDADE", "ADMINISTRATIVO", "EVIDENCIA_REVOGACAO", "EXAME", "ENCAMINHAMENTO")
                                .contains(category)
                        ? category
                        : "OUTRO";
        String classification = java.util.Set.of("IDENTIDADE", "ADMINISTRATIVO", "EVIDENCIA_REVOGACAO")
                        .contains(safeCategory)
                ? "ADMINISTRATIVO"
                : "CLINICO_RESTRITO";
        return new Patient.Attachment(
                UUID.randomUUID(),
                patient.id,
                safeCategory,
                classification,
                file.mediaType(),
                file.size(),
                file.storageKey(),
                actor.username(),
                patients.clock.instant(),
                false,
                "");
    }

    public UUID invalidate(Actor actor, UUID patientId, long version, UUID token, Map<String, String> form) {
        patients.authorize(actor, actor.isManager(), "INVALIDAR_ANEXO", patientId);
        return patients.write(actor, patientId, version, token, "INVALIDAR_ANEXO", form, p -> {
            text("reason", patients.value(form, "reason"), 500);
            UUID attachmentId = patients.uuid(form, "attachmentId");
            var attachment = p.attachments.stream()
                    .filter(a -> a.id().equals(attachmentId))
                    .findFirst()
                    .orElseThrow(() -> new RuleViolation("attachmentId", "Anexo não encontrado nesta ficha."));
            require("attachmentId", !attachment.invalid(), "O anexo já está invalidado.");
            p.attachments.set(
                    p.attachments.indexOf(attachment),
                    new Patient.Attachment(
                            attachment.id(),
                            attachment.origin(),
                            attachment.category(),
                            attachment.classification(),
                            attachment.mediaType(),
                            attachment.size(),
                            attachment.storageKey(),
                            attachment.author(),
                            attachment.createdAt(),
                            true,
                            patients.value(form, "reason")));
        });
    }

    public Download download(Actor actor, UUID id) {
        patients.authorize(actor, actor.canRead(), "LER_ANEXO", null);
        var owner = patients.store.all().stream()
                .filter(p -> p.mergedInto == null)
                .filter(p -> p.attachments.stream().anyMatch(a -> a.id().equals(id)))
                .findFirst();
        if (owner.isEmpty()) {
            patients.event(actor, "LER_ANEXO", null, "NEGADO", "", "", id.toString());
            throw new AccessDenied();
        }
        UUID patientId = owner.get().id;
        try {
            return patients.store.transaction(() -> {
                patients.store.lock(java.util.List.of(patientId));
                Patient p = patients.canonical(patientId);
                var attachment = p.attachments.stream()
                        .filter(a -> a.id().equals(id))
                        .findFirst()
                        .orElseThrow(AccessDenied::new);
                if (!attachment.administrative()) throw new AccessDenied();
                byte[] content = files.load(attachment.storageKey());
                require("file", content.length == attachment.size(), "Arquivo indisponível.");
                patients.event(actor, "LER_ANEXO", p.id, "SUCESSO", "", "", id.toString());
                return new Download(content, attachment.mediaType(), id);
            });
        } catch (AccessDenied denied) {
            patients.event(actor, "LER_ANEXO", patientId, "NEGADO", "", "", id.toString());
            throw denied;
        } catch (RuleViolation error) {
            patients.event(actor, "LER_ANEXO", patientId, "INDISPONIVEL", "", "", id.toString());
            throw error;
        }
    }

    public record Download(byte[] content, String mediaType, UUID id) {}
}
