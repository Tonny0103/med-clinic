package br.com.medclinic.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RulesTest {
    private final LocalDate today = LocalDate.of(2026, 10, 24);

    @Test
    void validatesCpfChecksumsWithoutLosingLeadingZeros() {
        assertThat(Rules.validCpf("000.000.001-91")).isTrue();
        assertThat(Rules.validCpf("00000000190")).isFalse();
        assertThat(Rules.validCpf("11111111111")).isFalse();
        assertThat(Rules.validCpf("x00000000191")).isFalse();
    }

    @Test
    void detectsOnlyTheSpecifiedDuplicatePatterns() {
        LocalDate date = LocalDate.of(1990, 3, 7);
        assertThat(Rules.normalize("  MÁRIA   Silva ")).isEqualTo("maria silva");
        assertThat(Rules.suspicious("Mária Silva", date, "Maria Silva", date)).isTrue();
        assertThat(Rules.suspicious("Maria Silva", date, "Maira Silva", date)).isTrue();
        assertThat(Rules.suspicious("Maria Silva", date, "Marina Silva", date)).isTrue();
        assertThat(Rules.suspicious("Maria Silva", date, "Marcos Silva", date)).isFalse();
        assertThat(Rules.suspicious("Maria Silva", date, "Maria Silva", LocalDate.of(1990, 7, 3)))
                .isTrue();
        assertThat(Rules.suspicious("Maria Silva", date, "Maira Silva", LocalDate.of(1990, 7, 3)))
                .isFalse();
    }

    @Test
    void requiresContactAndAllowsOptionalEmail() {
        Patient p = adult();
        assertThatCode(() -> Rules.validate(p, today)).doesNotThrowAnyException();
        p.phone = "";
        assertThatThrownBy(() -> Rules.validate(p, today)).isInstanceOf(RuleViolation.class);
        p.phone = "+55 65 90000-0000";
        p.address = "";
        assertThatThrownBy(() -> Rules.validate(p, today)).isInstanceOf(RuleViolation.class);
    }

    @Test
    void onlyMinorOrForeignerMayUseAlternativeIdentityAndMinorNeedsGuardian() {
        Patient p = adult();
        p.cpf = "";
        p.alternativeType = "PASSAPORTE";
        p.alternativeNumber = "DEMO-001";
        p.alternativeCountry = "BR";
        p.alternativeIssuer = "FICTICIO";
        p.alternativeReason = "Demonstração";
        assertThatThrownBy(() -> Rules.validate(p, today)).isInstanceOf(RuleViolation.class);
        p.nationality = "AR";
        assertThatCode(() -> Rules.validate(p, today)).doesNotThrowAnyException();
        p.birthdate = today.minusYears(10);
        assertThatThrownBy(() -> Rules.validate(p, today)).isInstanceOf(RuleViolation.class);
        p.guardians.add(new Patient.Guardian(
                UUID.randomUUID(), p.id, "Responsável fictício", "Mãe", "DOC-DEMO", "+55 65 90000-0000", today, null));
        assertThatCode(() -> Rules.validate(p, today)).doesNotThrowAnyException();
    }

    @Test
    void reconstructsPastCoverageAndTreatsExpiryAsInclusive() {
        Patient.Coverage coverage = new Patient.Coverage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Fictícia",
                "Demo",
                "CARD-DEMO",
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 1),
                null);
        assertThat(coverage.inForceOn(LocalDate.of(2026, 3, 31))).isTrue();
        assertThat(coverage.expiredOn(LocalDate.of(2026, 3, 31))).isFalse();
        assertThat(coverage.inForceOn(LocalDate.of(2026, 4, 1))).isFalse();
        assertThat(coverage.expiredOn(LocalDate.of(2026, 4, 1))).isTrue();
    }

    private Patient adult() {
        Patient p = new Patient();
        p.name = "Paciente fictício";
        p.birthdate = LocalDate.of(1990, 3, 7);
        p.cpf = "00000000191";
        p.phone = "+55 65 90000-0000";
        p.address = "Rua de demonstração, 1";
        return p;
    }
}
