package br.com.medclinic.presentation;

import br.com.medclinic.application.Actor;
import br.com.medclinic.application.PatientService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SharedModel {
    private final PatientService patients;

    public SharedModel(PatientService patients) {
        this.patients = patients;
    }

    @ModelAttribute("actor")
    Actor actor(Authentication auth) {
        return auth == null ? new Actor("", "") : WebSupport.actor(auth);
    }

    @ModelAttribute("today")
    LocalDate today() {
        return patients.today();
    }

    @ModelAttribute("token")
    UUID token() {
        return UUID.randomUUID();
    }
}
