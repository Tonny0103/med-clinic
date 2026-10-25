package br.com.medclinic.presentation;

import br.com.medclinic.application.ConsentService;
import br.com.medclinic.domain.RuleViolation;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ConsentController {
    private final ConsentService consents;

    public ConsentController(ConsentService consents) {
        this.consents = consents;
    }

    @PostMapping({"/pacientes/{id}/consentimentos", "/pacientes/{id}/revogacoes"})
    String record(
            @PathVariable UUID id,
            @RequestParam Map<String, String> form,
            @RequestParam(required = false) MultipartFile file,
            HttpServletRequest request,
            Authentication auth,
            RedirectAttributes flash)
            throws IOException {
        try {
            boolean selected = file != null
                    && (file.getSize() > 0
                            || file.getOriginalFilename() != null
                                    && !file.getOriginalFilename().isBlank());
            consents.record(
                    WebSupport.actor(auth),
                    id,
                    PatientController.version(form),
                    PatientController.token(form),
                    form,
                    request.getRequestURI().endsWith("/revogacoes"),
                    selected ? file.getBytes() : null,
                    file == null ? null : file.getContentType());
            return "redirect:/pacientes/" + id + "?sucesso";
        } catch (RuleViolation error) {
            flash.addFlashAttribute("message", error.getMessage());
            flash.addFlashAttribute("submitted", form);
            return "redirect:/pacientes/" + id;
        }
    }
}
