package br.com.medclinic.presentation;

import br.com.medclinic.application.RelationshipService;
import br.com.medclinic.domain.RuleViolation;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RelationshipController {
    private final RelationshipService relationships;

    public RelationshipController(RelationshipService relationships) {
        this.relationships = relationships;
    }

    @PostMapping("/pacientes/{id}/vinculos/{action}")
    String update(
            @PathVariable UUID id,
            @PathVariable String action,
            @RequestParam Map<String, String> form,
            Authentication auth,
            RedirectAttributes flash) {
        try {
            relationships.update(
                    WebSupport.actor(auth),
                    id,
                    PatientController.version(form),
                    PatientController.token(form),
                    action,
                    form);
            return "redirect:/pacientes/" + id + "?sucesso";
        } catch (RuleViolation error) {
            flash.addFlashAttribute("message", error.getMessage());
            flash.addFlashAttribute("submitted", form);
            return "redirect:/pacientes/" + id;
        }
    }
}
