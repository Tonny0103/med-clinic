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

    @GetMapping("/pacientes")
    String search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "ATIVO") String status,
            @RequestParam(defaultValue = "0") int page,
            Authentication auth,
            Model model) {
        model.addAttribute("page", patients.search(WebSupport.actor(auth), q, status, page));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        return "patients";
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
    String detail(
            @PathVariable UUID id,
            @RequestParam(required = false)
                    @org.springframework.format.annotation.DateTimeFormat(
                            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                    java.time.LocalDate reference,
            Authentication auth,
            Model model) {
        var view = patients.read(WebSupport.actor(auth), id);
        model.addAttribute("p", view.patient());
        model.addAttribute("originalChart", view.originalChart());
        model.addAttribute("history", view.history());
        model.addAttribute("consentStatuses", br.com.medclinic.application.ConsentService.statuses(view.patient()));
        model.addAttribute("reference", reference == null ? patients.today() : reference);
        model.addAttribute(
                "effectivePreference",
                br.com.medclinic.application.RelationshipService.effectivePreference(view.patient(), patients.today()));
        return "patient";
    }

    @GetMapping("/pacientes/{id}/editar")
    String editForm(@PathVariable UUID id, Authentication auth, Model model) {
        patients.requireWriter(WebSupport.actor(auth), "FORMULARIO_EDICAO", id);
        var p = patients.read(WebSupport.actor(auth), id).patient();
        br.com.medclinic.domain.Rules.require(
                "status", p.status.equals("ATIVO") && p.id.equals(id), "Abra um cadastro ativo para editar.");
        Map<String, String> form = PatientService.profileFields(p);
        form.put("version", Long.toString(p.version));
        model.addAttribute("form", form);
        model.addAttribute("editing", true);
        model.addAttribute("patientId", id);
        model.addAttribute("errors", Map.of());
        model.addAttribute(
                "evidenceOptions",
                p.attachments.stream().filter(a -> a.administrative()).toList());
        return "patient-form";
    }

    @PostMapping("/pacientes/{id}/editar")
    String edit(
            @PathVariable UUID id,
            @RequestParam Map<String, String> form,
            Authentication auth,
            Model model,
            HttpServletResponse response) {
        try {
            patients.edit(WebSupport.actor(auth), id, version(form), token(form), form);
            return "redirect:/pacientes/" + id + "?sucesso";
        } catch (RuleViolation error) {
            response.setStatus(422);
            model.addAttribute("form", form);
            model.addAttribute("errors", Map.of(error.field, error.getMessage()));
            model.addAttribute("editing", true);
            model.addAttribute("patientId", id);
            model.addAttribute(
                    "evidenceOptions",
                    patients.read(WebSupport.actor(auth), id).patient().attachments.stream()
                            .filter(a -> a.administrative())
                            .toList());
            return "patient-form";
        }
    }

    @PostMapping("/pacientes/{id}/situacao")
    String status(@PathVariable UUID id, @RequestParam Map<String, String> form, Authentication auth) {
        patients.changeStatus(WebSupport.actor(auth), id, version(form), token(form), form);
        return "redirect:/pacientes/" + id + "?sucesso";
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
