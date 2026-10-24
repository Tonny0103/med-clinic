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
