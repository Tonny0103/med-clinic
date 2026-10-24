package br.com.medclinic.application;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AccessPolicyTest {
    @Test
    void doctorReadsButCannotWriteOrManage() {
        Actor actor = new Actor("medico", "MEDICO");
        assertThat(actor.canRead()).isTrue();
        assertThat(actor.canWrite()).isFalse();
        assertThat(actor.isManager()).isFalse();
    }

    @Test
    void unknownRoleHasNoAdministrativeAccess() {
        assertThat(new Actor("unknown", "OTHER").canRead()).isFalse();
    }
}
