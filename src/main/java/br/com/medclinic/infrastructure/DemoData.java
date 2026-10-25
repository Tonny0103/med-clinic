package br.com.medclinic.infrastructure;

import br.com.medclinic.application.Actor;
import br.com.medclinic.application.ClinicStore;
import br.com.medclinic.application.PatientService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Explicitly opt-in fictitious data, only when the database is empty. */
@Component
@Profile("demo")
public class DemoData implements ApplicationRunner {
    private final ClinicStore store;
    private final PatientService patients;

    public DemoData(ClinicStore store, PatientService patients) {
        this.store = store;
        this.patients = patients;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!store.all().isEmpty()) return;
        Actor actor = new Actor("semente-demo", "GERENTE");
        for (int index = 1; index <= 24; index++) {
            Map<String, String> form = new HashMap<>();
            form.put("name", String.format("Paciente Demonstração %02d", index));
            form.put(
                    "birthdate",
                    LocalDate.of(1970 + index, 1 + index % 12, 1 + index % 27).toString());
            form.put("sex", "NAO_INFORMADO");
            form.put("nationality", "BR");
            form.put("cpf", fictitiousCpf(index + 1000));
            form.put("phone", "+55 65 90000-0000");
            form.put("address", "Rua Fictícia, " + index + " · Bairro Demonstração");
            form.put("country", "BR");
            patients.create(actor, UUID.randomUUID(), form);
        }
    }

    private String fictitiousCpf(int value) {
        StringBuilder digits = new StringBuilder(String.format("%09d", value));
        for (int length = 9; length <= 10; length++) {
            int sum = 0;
            for (int i = 0; i < length; i++) sum += (digits.charAt(i) - '0') * (length + 1 - i);
            int next = sum * 10 % 11;
            digits.append(next == 10 ? 0 : next);
        }
        return digits.toString();
    }
}
