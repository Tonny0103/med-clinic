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
    private final VisitHistory visits;

    public PatientService(ClinicStore store, Clock clock) {
        this(store, clock, id -> new VisitHistory.Result(VisitHistory.State.NAO_CONFIGURADO, java.util.List.of()));
    }

    public PatientService(ClinicStore store, Clock clock, VisitHistory visits) {
        this.store = store;
        this.clock = clock;
        this.visits = visits;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public UUID changeStatus(Actor actor, UUID id, long version, UUID token, Map<String, String> form) {
        return write(actor, id, version, token, "SITUACAO", form, p -> {
            br.com.medclinic.domain.Rules.text("reason", value(form, "reason"), 500);
            String target = value(form, "target");
            require(
                    "target",
                    java.util.Set.of("ATIVO", "INATIVO").contains(target)
                            && !"MESCLADO".equals(p.status)
                            && !target.equals(p.status),
                    "Transição inválida. Cadastros mesclados não podem ser reativados.");
            p.status = target;
        });
    }

    public UUID edit(Actor actor, UUID id, long version, UUID token, Map<String, String> form) {
        return write(actor, id, version, token, "EDITAR", form, p -> {
            active(p);
            String oldCpf = p.cpf;
            String oldAlternative = alternativeKey(p);
            String oldAlternativeNumber = p.alternativeNumber;
            applyProfile(p, form);
            boolean correction = !oldCpf.isBlank() && !oldCpf.equals(br.com.medclinic.domain.Rules.digits(p.cpf))
                    || !oldAlternativeNumber.isBlank() && !oldAlternative.equals(alternativeKey(p));
            if (correction) {
                br.com.medclinic.domain.Rules.text("reason", value(form, "reason"), 500);
                proof(p, uuid(form, "evidenceId"));
            }
            require(
                    "alternativeNumber",
                    !(oldCpf.isBlank()
                            && !p.cpf.isBlank()
                            && !oldAlternativeNumber.isBlank()
                            && p.alternativeNumber.isBlank()),
                    "Ao acrescentar CPF, preserve o documento alternativo anterior.");
            br.com.medclinic.domain.Rules.validate(p, today());
            checkSuspicion(p, value(form, "reason"));
        });
    }

    protected Patient.Attachment proof(Patient p, UUID evidence) {
        return p.attachments.stream()
                .filter(a -> a.id().equals(evidence) && a.administrative())
                .findFirst()
                .orElseThrow(() ->
                        new RuleViolation("evidenceId", "Selecione prova administrativa válida desta identidade."));
    }

    private String alternativeKey(Patient p) {
        return br.com.medclinic.domain.Rules.normalize(
                p.alternativeType + "|" + p.alternativeNumber + "|" + p.alternativeCountry + "|" + p.alternativeIssuer);
    }

    public static Map<String, String> profileFields(Patient p) {
        Map<String, String> f = new java.util.LinkedHashMap<>();
        f.put("name", p.name);
        f.put("birthdate", p.birthdate.toString());
        f.put("sex", p.sex);
        f.put("nationality", p.nationality);
        f.put("cpf", p.cpf);
        f.put("alternativeType", p.alternativeType);
        f.put("alternativeNumber", p.alternativeNumber);
        f.put("alternativeCountry", p.alternativeCountry);
        f.put("alternativeIssuer", p.alternativeIssuer);
        f.put("alternativeReason", p.alternativeReason);
        f.put("phone", p.phone);
        f.put("email", p.email);
        f.put("address", p.address);
        f.put("country", p.country);
        return f;
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
            return new PatientView(p, id.equals(p.id) ? "" : requested.chart, history(p));
        });
    }

    private VisitHistory.Result history(Patient patient) {
        try {
            java.util.Set<UUID> identities = new java.util.LinkedHashSet<>();
            identities.add(patient.id);
            patient.merges.forEach(merge -> identities.add(merge.sourceId()));
            var results = identities.stream().map(visits::query).toList();
            if (results.stream().allMatch(r -> r.state() == VisitHistory.State.NAO_CONFIGURADO))
                return new VisitHistory.Result(VisitHistory.State.NAO_CONFIGURADO, java.util.List.of());
            if (results.stream()
                    .anyMatch(r ->
                            r.state() == VisitHistory.State.FALHA || r.state() == VisitHistory.State.NAO_CONFIGURADO))
                return new VisitHistory.Result(VisitHistory.State.FALHA, java.util.List.of());
            var entries = results.stream()
                    .flatMap(r -> r.entries().stream())
                    .sorted(java.util.Comparator.comparing(VisitHistory.Entry::date)
                            .reversed())
                    .toList();
            return new VisitHistory.Result(
                    entries.isEmpty() ? VisitHistory.State.VAZIO : VisitHistory.State.DISPONIVEL, entries);
        } catch (RuntimeException unavailable) {
            return new VisitHistory.Result(VisitHistory.State.FALHA, java.util.List.of());
        }
    }

    public record PatientView(Patient patient, String originalChart, VisitHistory.Result history) {}

    public record SearchHit(
            UUID id, String name, LocalDate birthdate, String maskedCpf, String chart, String status, String alias) {}

    public Page<SearchHit> search(Actor actor, String query, String status, int page) {
        authorize(actor, actor.canRead(), "BUSCAR", null);
        String q = br.com.medclinic.domain.Rules.normalize(query);
        require("q", q.length() <= 160, "Use até 160 caracteres na busca.");
        boolean exact = query.matches("[0-9.\\-\\s]+")
                && java.util.Set.of(11, 12)
                        .contains(br.com.medclinic.domain.Rules.digits(query).length());
        return store.transaction(() -> {
            var hits = new java.util.LinkedHashMap<UUID, SearchHit>();
            String identifier = br.com.medclinic.domain.Rules.digits(query);
            UUID cpfOwner = exact && identifier.length() == 11
                    ? store.findByCpf(identifier).orElse(null)
                    : null;
            if (q.length() >= 2) {
                for (Patient candidate : store.all()) {
                    boolean match = exact
                            ? identifier.length() == 12
                                    ? candidate.chart.equals(identifier)
                                    : candidate.id.equals(cpfOwner)
                            : candidate.mergedInto == null
                                    && br.com.medclinic.domain.Rules.normalize(candidate.name)
                                            .contains(q)
                                    && ("TODOS".equals(status) || candidate.status.equals(status));
                    if (match) {
                        Patient p = candidate.mergedInto == null ? candidate : canonical(candidate.id);
                        hits.put(
                                p.id,
                                new SearchHit(
                                        candidate.id,
                                        p.name,
                                        p.birthdate,
                                        br.com.medclinic.domain.Rules.maskedCpf(p.cpf),
                                        p.chart,
                                        p.status,
                                        candidate.id.equals(p.id) ? "" : candidate.chart));
                    }
                }
            }
            var sorted = hits.values().stream()
                    .sorted(java.util.Comparator.comparing(
                                    (SearchHit h) -> br.com.medclinic.domain.Rules.normalize(h.name()))
                            .thenComparing(SearchHit::chart))
                    .toList();
            event(actor, "BUSCAR", null, sorted.isEmpty() ? "VAZIO" : "SUCESSO", "", "", "");
            return Page.of(sorted, page);
        });
    }

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
                Map<String, String> before = profileFields(p);
                mutation.accept(p);
                Map<String, String> after = profileFields(p);
                String fields = operation.equals("EDITAR")
                        ? String.join(
                                ",",
                                before.keySet().stream()
                                        .filter(key -> !before.get(key).equals(after.get(key)))
                                        .toList())
                        : auditFields(form);
                store.save(p, version);
                event(
                        actor,
                        operation,
                        p.id,
                        "SUCESSO",
                        fields,
                        form.getOrDefault("reason", ""),
                        auditReference(form.getOrDefault("evidenceId", "")));
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

    private String auditReference(String value) {
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException error) {
            return "";
        }
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
