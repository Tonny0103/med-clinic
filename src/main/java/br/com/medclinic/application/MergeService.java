package br.com.medclinic.application;

import static br.com.medclinic.domain.Rules.*;

import br.com.medclinic.domain.Patient;
import br.com.medclinic.domain.RuleViolation;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MergeService {
    private final PatientService patients;
    private final Map<UUID, Preview> previews = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> consumed = ConcurrentHashMap.newKeySet();
    private static final List<String> FIELDS =
            List.of("sex", "nationality", "phone", "email", "address", "country", "emergency", "preference");

    public MergeService(PatientService patients) {
        this.patients = patients;
    }

    public record Preview(
            UUID id,
            UUID sourceId,
            UUID destinationId,
            long sourceVersion,
            long destinationVersion,
            UUID sourceEvidenceId,
            UUID destinationEvidenceId,
            Map<String, String> choices,
            String reason,
            String actor,
            Instant expiresAt,
            Map<String, String> resultingConsents) {}

    public record Conflict(String field, String label, String source, String destination) {}

    public record Comparison(Patient source, Patient destination, List<Conflict> conflicts) {}

    public List<PatientService.SearchHit> candidates(Actor actor) {
        patients.authorize(actor, actor.isManager(), "PREPARAR_MESCLAGEM", null);
        return patients.store.transaction(() -> {
            var rows = patients.store.all().stream()
                    .filter(p -> p.mergedInto == null)
                    .map(p -> new PatientService.SearchHit(
                            p.id, p.name, p.birthdate, maskedCpf(p.cpf), p.chart, p.status, ""))
                    .toList();
            patients.event(actor, "PREPARAR_MESCLAGEM", null, "SUCESSO", "", "", "");
            return rows;
        });
    }

    public Comparison compare(Actor actor, UUID source, UUID destination) {
        patients.authorize(actor, actor.isManager(), "COMPARAR_MESCLAGEM", destination);
        return patients.store.transaction(() -> {
            patients.store.lock(List.of(source, destination));
            Patient s = root(source), d = root(destination);
            require("sourceId", !source.equals(destination), "Escolha dois cadastros diferentes.");
            patients.event(actor, "COMPARAR_MESCLAGEM", destination, "SUCESSO", "", "", source.toString());
            return new Comparison(s, d, conflicts(s, d));
        });
    }

    public Preview preview(
            Actor actor,
            UUID source,
            UUID destination,
            long sourceVersion,
            long destinationVersion,
            UUID sourceEvidence,
            UUID destinationEvidence,
            Map<String, String> choices,
            String reason) {
        patients.authorize(actor, actor.isManager(), "PREVIA_MESCLAGEM", destination);
        try {
            Preview preview = patients.store.transaction(() -> {
                patients.store.lock(List.of(source, destination));
                Patient s = patients.current(source, sourceVersion),
                        d = patients.current(destination, destinationVersion);
                validateIdentity(s, d, sourceEvidence, destinationEvidence);
                text("reason", reason, 500);
                validateChoices(s, d, choices);
                var next = new Preview(
                        UUID.randomUUID(),
                        source,
                        destination,
                        sourceVersion,
                        destinationVersion,
                        sourceEvidence,
                        destinationEvidence,
                        Map.copyOf(choices),
                        reason,
                        actor.username(),
                        patients.clock.instant().plusSeconds(15 * 60),
                        Map.of());
                combine(s, d, next);
                validate(d, patients.today());
                patients.event(
                        actor,
                        "PREVIA_MESCLAGEM",
                        destination,
                        "SUCESSO",
                        "",
                        reason,
                        sourceEvidence + ";" + destinationEvidence);
                return new Preview(
                        next.id(),
                        source,
                        destination,
                        sourceVersion,
                        destinationVersion,
                        sourceEvidence,
                        destinationEvidence,
                        next.choices(),
                        reason,
                        actor.username(),
                        next.expiresAt(),
                        ConsentService.statuses(d));
            });
            Instant now = patients.clock.instant();
            previews.entrySet().removeIf(e -> {
                boolean expired = !e.getValue().expiresAt().isAfter(now);
                if (expired) consumed.remove(e.getKey());
                return expired;
            });
            previews.put(preview.id(), preview);
            return preview;
        } catch (RuleViolation error) {
            patients.event(actor, "PREVIA_MESCLAGEM", destination, "RECUSADO", "", "", "");
            throw error;
        }
    }

    public UUID confirm(Actor actor, UUID previewId, UUID token, boolean confirmed) {
        patients.authorize(actor, actor.isManager(), "MESCLAR", null);
        String fingerprint = patients.fingerprint(
                Map.of("previewId", previewId.toString(), "confirmed", Boolean.toString(confirmed)));
        try {
            require("confirmed", confirmed, "Confirme explicitamente a identidade e a consolidação.");
            var receipt = patients.store.receipt(actor.username(), token);
            if (receipt.isPresent()) return replay(receipt.get(), fingerprint);
            Preview preview = previews.get(previewId);
            require("previewId", preview != null, "Prévia inexistente ou expirada. Prepare uma nova.");
            require("previewId", preview.actor().equals(actor.username()), "A prévia pertence a outro gerente.");
            UUID result = patients.store.transaction(() -> {
                patients.store.lock(List.of(preview.sourceId(), preview.destinationId()));
                var previous = patients.store.receipt(actor.username(), token);
                if (previous.isPresent()) return replay(previous.get(), fingerprint);
                require(
                        "previewId",
                        !consumed.contains(previewId) && preview.expiresAt().isAfter(patients.clock.instant()),
                        "Prévia usada ou expirada. Prepare uma nova.");
                Patient s = patients.current(preview.sourceId(), preview.sourceVersion());
                Patient d = patients.current(preview.destinationId(), preview.destinationVersion());
                validateIdentity(s, d, preview.sourceEvidenceId(), preview.destinationEvidenceId());
                validateChoices(s, d, preview.choices());
                combine(s, d, preview);
                validate(d, patients.today());
                s.status = "MESCLADO";
                s.mergedInto = d.id;
                patients.store.save(s, preview.sourceVersion());
                patients.store.save(d, preview.destinationVersion());
                String references = "source=" + s.id + ";sourceProof=" + preview.sourceEvidenceId()
                        + ";destinationProof=" + preview.destinationEvidenceId();
                patients.event(
                        actor,
                        "MESCLAR",
                        d.id,
                        "SUCESSO",
                        "mergedInto,guardians,coverages,consents,attachments,"
                                + String.join(",", preview.choices().keySet()),
                        preview.reason(),
                        references);
                patients.event(
                        actor, "MESCLAR_ORIGEM", s.id, "SUCESSO", "status,mergedInto", preview.reason(), references);
                patients.store.receipt(actor.username(), token, new ClinicStore.Receipt("MESCLAR", fingerprint, d.id));
                return d.id;
            });
            consumed.add(previewId);
            return result;
        } catch (RuleViolation error) {
            patients.event(actor, "MESCLAR", null, "RECUSADO", "", "", previewId.toString());
            throw error;
        }
    }

    private UUID replay(ClinicStore.Receipt receipt, String fingerprint) {
        require(
                "token",
                receipt.operation().equals("MESCLAR") && receipt.fingerprint().equals(fingerprint),
                "Submissão reutilizada com dados diferentes.");
        return receipt.patientId();
    }

    private Patient root(UUID id) {
        Patient p = patients.canonical(id);
        require("patient", p.id.equals(id), "Cadastro já mesclado. Selecione a identidade consolidada.");
        return p;
    }

    private void validateIdentity(Patient s, Patient d, UUID sourceProof, UUID destinationProof) {
        require("sourceId", !s.id.equals(d.id), "Não é possível mesclar o mesmo cadastro.");
        require(
                "cpf",
                s.cpf.isBlank() || d.cpf.isBlank() || s.cpf.equals(d.cpf),
                "CPFs divergentes bloqueiam a mesclagem. Corrija documentalmente antes de continuar.");
        require(
                "name",
                normalize(s.name).equals(normalize(d.name)) && s.birthdate.equals(d.birthdate),
                "Nome completo e nascimento devem coincidir após correções autorizadas. Semelhança não comprova identidade.");
        patients.proof(s, sourceProof);
        patients.proof(d, destinationProof);
    }

    private List<Conflict> conflicts(Patient s, Patient d) {
        Map<String, String> a = values(s), b = values(d);
        return FIELDS.stream()
                .filter(f -> !a.get(f).equals(b.get(f)))
                .map(f -> new Conflict(f, label(f), a.get(f), b.get(f)))
                .toList();
    }

    private void validateChoices(Patient s, Patient d, Map<String, String> choices) {
        require("choices", FIELDS.containsAll(choices.keySet()), "Escolha administrativa inválida.");
        for (Conflict conflict : conflicts(s, d))
            require(
                    "choices",
                    java.util.Set.of("SOURCE", "DESTINATION").contains(choices.getOrDefault(conflict.field(), "")),
                    "Resolva o campo: " + conflict.label());
    }

    private Map<String, String> values(Patient p) {
        Map<String, String> map = new java.util.LinkedHashMap<>(PatientService.profileFields(p));
        map.put(
                "emergency",
                p.emergency == null
                        ? "Não informado"
                        : p.emergency.name() + " · " + p.emergency.relationship() + " · " + p.emergency.phone());
        map.put("preference", RelationshipService.effectivePreference(p, patients.today()));
        return map;
    }

    private String label(String field) {
        return switch (field) {
            case "sex" -> "Sexo informado";
            case "nationality" -> "Nacionalidade";
            case "phone" -> "Telefone";
            case "email" -> "E-mail";
            case "address" -> "Endereço";
            case "country" -> "País do endereço";
            case "emergency" -> "Emergência";
            case "preference" -> "Preferência atual";
            default -> field;
        };
    }

    private void combine(Patient source, Patient destination, Preview preview) {
        var f = PatientService.profileFields(destination);
        var other = PatientService.profileFields(source);
        preview.choices().forEach((field, choice) -> {
            if (choice.equals("SOURCE") && other.containsKey(field)) f.put(field, other.get(field));
        });
        patients.applyProfile(destination, f);
        if ("SOURCE".equals(preview.choices().get("emergency"))) destination.emergency = source.emergency;
        if ("SOURCE".equals(preview.choices().get("preference"))) destination.preference = source.preference;
        if (destination.cpf.isBlank()) destination.cpf = source.cpf;
        if (destination.alternativeNumber.isBlank() && !source.alternativeNumber.isBlank()) {
            destination.alternativeType = source.alternativeType;
            destination.alternativeNumber = source.alternativeNumber;
            destination.alternativeCountry = source.alternativeCountry;
            destination.alternativeIssuer = source.alternativeIssuer;
            destination.alternativeReason = source.alternativeReason;
        }
        destination.guardians.addAll(source.guardians);
        destination.coverages.addAll(source.coverages);
        destination.consents.addAll(source.consents);
        destination.attachments.addAll(source.attachments);
        destination.merges.addAll(source.merges);
        destination.merges.add(new Patient.Merge(
                source.id,
                source.chart,
                preview.sourceEvidenceId(),
                preview.destinationEvidenceId(),
                preview.reason(),
                new java.util.TreeMap<>(preview.choices()).toString(),
                preview.actor(),
                patients.clock.instant(),
                preview.destinationVersion() + 1,
                destination.id));
        destination.preference = RelationshipService.effectivePreference(destination, patients.today());
    }
}
