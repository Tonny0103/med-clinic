package br.com.medclinic.presentation;

import br.com.medclinic.application.AttachmentService;
import br.com.medclinic.domain.RuleViolation;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AttachmentController {
    private final AttachmentService attachments;

    public AttachmentController(AttachmentService attachments) {
        this.attachments = attachments;
    }

    @PostMapping("/pacientes/{id}/anexos")
    String upload(
            @PathVariable UUID id,
            @RequestParam Map<String, String> form,
            @RequestParam MultipartFile file,
            Authentication auth,
            RedirectAttributes flash)
            throws IOException {
        try {
            attachments.upload(
                    WebSupport.actor(auth),
                    id,
                    PatientController.version(form),
                    PatientController.token(form),
                    form,
                    file.getBytes(),
                    file.getContentType());
            return "redirect:/pacientes/" + id + "?sucesso";
        } catch (RuleViolation error) {
            flash.addFlashAttribute("message", error.getMessage());
            flash.addFlashAttribute("submitted", form);
            return "redirect:/pacientes/" + id;
        }
    }

    @PostMapping("/pacientes/{id}/anexos/invalidar")
    String invalidate(@PathVariable UUID id, @RequestParam Map<String, String> form, Authentication auth) {
        attachments.invalidate(
                WebSupport.actor(auth), id, PatientController.version(form), PatientController.token(form), form);
        return "redirect:/pacientes/" + id + "?sucesso";
    }

    @GetMapping("/anexos/{id}")
    ResponseEntity<byte[]> download(@PathVariable UUID id, Authentication auth) {
        var file = attachments.download(WebSupport.actor(auth), id);
        String extension =
                switch (file.mediaType()) {
                    case "application/pdf" -> ".pdf";
                    case "image/png" -> ".png";
                    default -> ".jpg";
                };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mediaType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("documento-" + id + extension)
                                .build()
                                .toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(file.content());
    }
}
