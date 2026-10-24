package br.com.medclinic.presentation;

import br.com.medclinic.application.MergeService;
import br.com.medclinic.domain.RuleViolation;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MergeController {
    private final MergeService merges;

    public MergeController(MergeService merges) {
        this.merges = merges;
    }

    @GetMapping("/mesclagem")
    String select(Authentication auth, Model model) {
        model.addAttribute("candidates", merges.candidates(WebSupport.actor(auth)));
        model.addAttribute("mode", "select");
        return "merge";
    }

    @PostMapping("/mesclagem/comparar")
    String compare(@RequestParam UUID sourceId, @RequestParam UUID destinationId, Authentication auth, Model model) {
        model.addAttribute("comparison", merges.compare(WebSupport.actor(auth), sourceId, destinationId));
        model.addAttribute("mode", "compare");
        return "merge";
    }

    @PostMapping("/mesclagem/previa")
    String preview(@RequestParam Map<String, String> form, Authentication auth, Model model) {
        Map<String, String> choices = new java.util.HashMap<>();
        form.forEach((key, value) -> {
            if (key.startsWith("choice.")) choices.put(key.substring(7), value);
        });
        UUID source = UUID.fromString(form.get("sourceId")), destination = UUID.fromString(form.get("destinationId"));
        var preview = merges.preview(
                WebSupport.actor(auth),
                source,
                destination,
                Long.parseLong(form.get("sourceVersion")),
                Long.parseLong(form.get("destinationVersion")),
                UUID.fromString(form.get("sourceEvidenceId")),
                UUID.fromString(form.get("destinationEvidenceId")),
                choices,
                form.getOrDefault("reason", ""));
        model.addAttribute("preview", preview);
        model.addAttribute("comparison", merges.compare(WebSupport.actor(auth), source, destination));
        model.addAttribute("mode", "preview");
        return "merge";
    }

    @PostMapping("/mesclagem/confirmar")
    String confirm(
            @RequestParam UUID previewId,
            @RequestParam Map<String, String> form,
            Authentication auth,
            RedirectAttributes flash) {
        try {
            UUID id = merges.confirm(
                    WebSupport.actor(auth),
                    previewId,
                    PatientController.token(form),
                    "sim".equals(form.get("confirmed")));
            return "redirect:/pacientes/" + id + "?sucesso";
        } catch (RuleViolation error) {
            flash.addFlashAttribute("message", error.getMessage());
            return "redirect:/mesclagem";
        }
    }
}
