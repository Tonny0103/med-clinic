package br.com.medclinic.presentation;

import br.com.medclinic.application.PatientService;
import br.com.medclinic.domain.RuleViolation;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PatientController {
    private final PatientService patients;

    public PatientController(PatientService patients) {
        this.patients = patients;
    }

    @GetMapping("/pacientes/novo")
    String newPatient(Authentication auth, Model model) {
        patients.requireWriter(WebSupport.actor(auth), "FORMULARIO_CADASTRO", null);
        model.addAttribute("form", new HashMap<>(Map.of("sex", "NAO_INFORMADO", "nationality", "BR", "country", "BR")));
        model.addAttribute("errors", Map.of());
        model.addAttribute("editing", false);
        return "patient-form";
    }

    @PostMapping("/pacientes")
    String create(
            @RequestParam Map<String, String> form, Authentication auth, Model model, HttpServletResponse response) {
        try {
            UUID id = patients.create(WebSupport.actor(auth), token(form), form);
            return "redirect:/pacientes/" + id + "?sucesso";
        } catch (RuleViolation error) {
            response.setStatus(422);
            model.addAttribute("form", form);
            model.addAttribute("errors", Map.of(error.field, error.getMessage()));
            model.addAttribute("editing", false);
            return "patient-form";
        }
    }

    @GetMapping("/pacientes/{id}")
    String detail(@PathVariable UUID id, Authentication auth, Model model) {
        var view = patients.read(WebSupport.actor(auth), id);
        model.addAttribute("p", view.patient());
        model.addAttribute("originalChart", view.originalChart());
        return "patient";
    }

    static UUID token(Map<String, String> form) {
        try {
            return UUID.fromString(form.getOrDefault("token", ""));
        } catch (IllegalArgumentException error) {
            throw new RuleViolation("token", "Reabra o formulário e tente novamente.");
        }
    }

    static long version(Map<String, String> form) {
        try {
            return Long.parseLong(form.getOrDefault("version", ""));
        } catch (NumberFormatException error) {
            throw new RuleViolation("version", "Reabra a ficha e revise os dados.");
        }
    }
}
