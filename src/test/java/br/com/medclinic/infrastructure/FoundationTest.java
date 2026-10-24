package br.com.medclinic.infrastructure;

import static org.assertj.core.api.Assertions.*;

import br.com.medclinic.application.ClinicStore;
import br.com.medclinic.domain.Patient;
import br.com.medclinic.domain.RuleViolation;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class FoundationTest {
    @Autowired
    ClinicStore store;

    @DynamicPropertySource
    static void passwords(DynamicPropertyRegistry registry) {
        registry.add("clinic.reception-password", () -> UUID.randomUUID().toString());
        registry.add("clinic.manager-password", () -> UUID.randomUUID().toString());
        registry.add("clinic.doctor-password", () -> UUID.randomUUID().toString());
    }

    @Test
    void failedTransactionLeavesNeitherPatientNorSuccessAudit() {
        Patient patient = fixture();
        UUID eventId = UUID.randomUUID();
        assertThatThrownBy(() -> store.transaction(() -> {
                    store.save(patient, -1);
                    store.audit(new ClinicStore.Event(
                            eventId,
                            Instant.now(),
                            "teste",
                            "GERENTE",
                            "CRIAR",
                            patient.id,
                            "SUCESSO",
                            "name",
                            "",
                            ""));
                    throw new RuleViolation("test", "Falha controlada");
                }))
                .isInstanceOf(RuleViolation.class);
        assertThat(store.find(patient.id)).isEmpty();
        assertThat(store.events()).noneMatch(event -> event.id().equals(eventId));
    }

    @Test
    void reservesIdentityAndRejectsStaleVersion() {
        Patient patient = fixture();
        store.transaction(() -> {
            store.save(patient, -1);
            return null;
        });
        Patient competing = fixture();
        competing.cpf = patient.cpf;
        assertThatThrownBy(() -> store.transaction(() -> {
                    store.save(competing, -1);
                    return null;
                }))
                .isInstanceOf(RuleViolation.class);
        assertThat(store.find(competing.id)).isEmpty();
        Patient stale = store.find(patient.id).orElseThrow();
        store.transaction(() -> {
            store.save(patient, 1);
            return null;
        });
        assertThatThrownBy(() -> store.transaction(() -> {
                    store.save(stale, 1);
                    return null;
                }))
                .isInstanceOf(RuleViolation.class);
        assertThat(store.find(patient.id).orElseThrow().version).isEqualTo(2);
    }

    private Patient fixture() {
        Patient p = new Patient();
        p.chart = store.nextChart();
        p.name = "Paciente fictício " + p.chart;
        p.birthdate = LocalDate.of(1990, 1, 1);
        p.cpf = "00000000272";
        return p;
    }
}
