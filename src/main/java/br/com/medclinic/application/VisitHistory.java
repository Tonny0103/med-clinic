package br.com.medclinic.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@FunctionalInterface
public interface VisitHistory {
    Result query(UUID patientId);

    enum State {
        NAO_CONFIGURADO,
        DISPONIVEL,
        VAZIO,
        FALHA
    }

    record Entry(LocalDate date, String professional, String type, UUID origin) {}

    record Result(State state, List<Entry> entries) {
        public Result {
            entries = List.copyOf(entries);
        }
    }
}
