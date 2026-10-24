package br.com.medclinic.application;

import br.com.medclinic.domain.Patient;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public interface ClinicStore {
    <T> T transaction(Supplier<T> operation);

    List<Patient> all();

    Optional<Patient> find(UUID id);

    void lock(List<UUID> ids);

    String nextChart();

    void save(Patient patient, long expectedVersion);

    Optional<Receipt> receipt(String actor, UUID token);

    void receipt(String actor, UUID token, Receipt receipt);

    void audit(Event event);

    List<Event> events();

    record Receipt(String operation, String fingerprint, UUID patientId) {}

    record Event(
            UUID id,
            Instant at,
            String actor,
            String role,
            String operation,
            UUID patientId,
            String outcome,
            String fields,
            String reason,
            String references) {}
}
