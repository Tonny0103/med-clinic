package br.com.medclinic.presentation;

import br.com.medclinic.application.Actor;
import org.springframework.security.core.Authentication;

public final class WebSupport {
    private WebSupport() {}

    public static Actor actor(Authentication auth) {
        String role = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("");
        return new Actor(auth.getName(), role);
    }
}
