package br.com.medclinic.application;

import java.util.Set;

public record Actor(String username, String role) {
    public boolean canRead() {
        return Set.of("RECEPCAO", "GERENTE", "MEDICO").contains(role);
    }

    public boolean canWrite() {
        return Set.of("RECEPCAO", "GERENTE").contains(role);
    }

    public boolean isManager() {
        return "GERENTE".equals(role);
    }
}
