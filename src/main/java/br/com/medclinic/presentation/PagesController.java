package br.com.medclinic.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PagesController {
    @GetMapping("/")
    String home() {
        return "redirect:/pacientes";
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }
}
