package br.com.medclinic.infrastructure;

import br.com.medclinic.application.ClinicStore;
import br.com.medclinic.domain.Patient;
import br.com.medclinic.domain.RuleViolation;
import br.com.medclinic.domain.Rules;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public class JdbcClinicStore implements ClinicStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    public JdbcClinicStore(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    @Override
    public <T> T transaction(Supplier<T> operation) {
        return transactions.execute(status -> operation.get());
    }

    @Override
    public List<Patient> all() {
        return jdbc.query(
                "select payload from patients order by name_normalized, chart",
                (row, number) -> decode(row.getString(1)));
    }

    @Override
    public Optional<Patient> find(UUID id) {
        return jdbc
                .query(
                        "select payload from patients where id = ?",
                        (row, number) -> decode(row.getString(1)),
                        id.toString())
                .stream()
                .findFirst();
    }

    @Override
    public void lock(List<UUID> ids) {
        ids.stream()
                .distinct()
                .sorted()
                .forEach(id -> jdbc.queryForList("select id from patients where id = ? for update", id.toString()));
    }

    @Override
    public String nextChart() {
        Long next = jdbc.queryForObject("select nextval('chart_sequence')", Long.class);
        return String.format("%012d", next);
    }

    @Override
    public void save(Patient p, long expectedVersion) {
        p.version = expectedVersion < 0 ? 1 : expectedVersion + 1;
        if (expectedVersion < 0) {
            jdbc.update(
                    "insert into patients(id, chart, name_normalized, birth_date, status, merged_into, version, payload) values(?,?,?,?,?,?,?,?)",
                    p.id.toString(),
                    p.chart,
                    Rules.normalize(p.name),
                    p.birthdate,
                    p.status,
                    p.mergedInto == null ? null : p.mergedInto.toString(),
                    p.version,
                    encode(p));
        } else {
            int changed = jdbc.update(
                    "update patients set name_normalized=?, birth_date=?, status=?, merged_into=?, version=?, payload=? where id=? and version=?",
                    Rules.normalize(p.name),
                    p.birthdate,
                    p.status,
                    p.mergedInto == null ? null : p.mergedInto.toString(),
                    p.version,
                    encode(p),
                    p.id.toString(),
                    expectedVersion);
            if (changed != 1) throw new RuleViolation("version", "A ficha foi alterada. Reabra e revise os dados.");
        }
        jdbc.update("delete from identities where patient_id = ?", p.id.toString());
        for (String key : identityKeys(p)) {
            List<String> owners =
                    jdbc.queryForList("select patient_id from identities where document_key = ?", String.class, key);
            if (owners.isEmpty()) {
                jdbc.update("insert into identities(document_key, patient_id) values(?,?)", key, p.id.toString());
            } else if (!canonicalId(UUID.fromString(owners.getFirst())).equals(canonicalId(p.id))) {
                throw new RuleViolation(
                        "cpf", "Documento já associado a outro cadastro, inclusive inativo ou mesclado.");
            }
        }
    }

    private UUID canonicalId(UUID id) {
        Patient p = find(id).orElseThrow();
        int hops = 0;
        while (p.mergedInto != null && hops++ < 100) p = find(p.mergedInto).orElseThrow();
        return p.id;
    }

    private List<String> identityKeys(Patient p) {
        List<String> keys = new ArrayList<>();
        if (!p.cpf.isBlank()) keys.add("CPF:" + Rules.digits(p.cpf));
        if (!p.alternativeNumber.isBlank())
            keys.add("ALT:" + Rules.normalize(p.alternativeType) + "|"
                    + Rules.normalize(p.alternativeCountry) + "|" + Rules.normalize(p.alternativeIssuer) + "|"
                    + Rules.normalize(p.alternativeNumber));
        return keys;
    }

    @Override
    public Optional<Receipt> receipt(String actor, UUID token) {
        return jdbc
                .query(
                        "select operation, fingerprint, patient_id from receipts where actor=? and token=?",
                        (r, n) -> new Receipt(r.getString(1), r.getString(2), UUID.fromString(r.getString(3))),
                        actor,
                        token.toString())
                .stream()
                .findFirst();
    }

    @Override
    public void receipt(String actor, UUID token, Receipt receipt) {
        jdbc.update(
                "insert into receipts(actor,token,operation,fingerprint,patient_id) values(?,?,?,?,?)",
                actor,
                token.toString(),
                receipt.operation(),
                receipt.fingerprint(),
                receipt.patientId().toString());
    }

    @Override
    public void audit(Event event) {
        jdbc.update(
                "insert into audit_events(id,occurred_at,actor,role,operation,patient_id,outcome,fields,reason,refs) values(?,?,?,?,?,?,?,?,?,?)",
                event.id().toString(),
                Timestamp.from(event.at()),
                event.actor(),
                event.role(),
                event.operation(),
                event.patientId() == null ? null : event.patientId().toString(),
                event.outcome(),
                event.fields(),
                event.reason(),
                event.references());
    }

    @Override
    public List<Event> events() {
        return jdbc.query("select * from audit_events order by occurred_at desc, id desc", (r, n) -> event(r));
    }

    private Event event(ResultSet row) throws SQLException {
        String patient = row.getString("patient_id");
        return new Event(
                UUID.fromString(row.getString("id")),
                row.getTimestamp("occurred_at").toInstant(),
                row.getString("actor"),
                row.getString("role"),
                row.getString("operation"),
                patient == null ? null : UUID.fromString(patient),
                row.getString("outcome"),
                row.getString("fields"),
                row.getString("reason"),
                row.getString("refs"));
    }

    private String encode(Patient patient) {
        try {
            return json.writeValueAsString(patient);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível preparar o cadastro.", exception);
        }
    }

    private Patient decode(String value) {
        try {
            return json.readValue(value, Patient.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível carregar o cadastro.", exception);
        }
    }
}
