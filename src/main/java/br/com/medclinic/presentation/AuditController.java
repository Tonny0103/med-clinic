package br.com.medclinic.presentation;

import br.com.medclinic.application.AuditService;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuditController {
    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping("/auditoria")
    String query(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            Authentication auth,
            Model model) {
        var report = audit.query(WebSupport.actor(auth), filters, page);
        model.addAttribute("page", report.page());
        model.addAttribute("patients", report.patients());
        model.addAttribute("filters", filters);
        return "audit";
    }
}
