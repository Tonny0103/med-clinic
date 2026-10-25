package br.com.medclinic.infrastructure;

import br.com.medclinic.application.ClinicStore;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(
            @Value("${clinic.reception-password}") String reception,
            @Value("${clinic.manager-password}") String manager,
            @Value("${clinic.doctor-password}") String doctor,
            BCryptPasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("recepcao")
                        .password(encoder.encode(secret(reception, "RECEPTION_PASSWORD")))
                        .roles("RECEPCAO")
                        .build(),
                User.withUsername("gerente")
                        .password(encoder.encode(secret(manager, "MANAGER_PASSWORD")))
                        .roles("GERENTE")
                        .build(),
                User.withUsername("medico")
                        .password(encoder.encode(secret(doctor, "DOCTOR_PASSWORD")))
                        .roles("MEDICO")
                        .build());
    }

    private String secret(String value, String variable) {
        if (value == null || value.length() < 12)
            throw new IllegalStateException("Configure " + variable + " com ao menos 12 caracteres.");
        return value;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, ClinicStore store) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/login", "/style.css", "/error")
                        .permitAll()
                        .requestMatchers("/mesclagem", "/mesclagem/**", "/auditoria")
                        .hasRole("GERENTE")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/pacientes/**")
                        .hasAnyRole("RECEPCAO", "GERENTE")
                        .requestMatchers("/pacientes/*/editar")
                        .hasAnyRole("RECEPCAO", "GERENTE")
                        .anyRequest()
                        .hasAnyRole("RECEPCAO", "GERENTE", "MEDICO"))
                .formLogin(form -> form.loginPage("/login")
                        .defaultSuccessUrl("/pacientes", true)
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .csrf(Customizer.withDefaults())
                .headers(
                        headers -> headers.contentSecurityPolicy(
                                csp -> csp.policyDirectives(
                                        "default-src 'self'; script-src 'none'; object-src 'none'; frame-ancestors 'none'; form-action 'self'; base-uri 'self'")))
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) -> {
                    var auth = org.springframework.security.core.context.SecurityContextHolder.getContext()
                            .getAuthentication();
                    store.audit(new ClinicStore.Event(
                            UUID.randomUUID(),
                            Instant.now(),
                            auth == null ? "ANONIMO" : auth.getName(),
                            auth == null
                                    ? "ANONIMO"
                                    : auth.getAuthorities().stream()
                                            .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                                            .findFirst()
                                            .orElse("SEM_PERFIL"),
                            "ACESSO_NEGADO",
                            null,
                            "NEGADO",
                            "",
                            "",
                            ""));
                    response.sendError(403);
                }));
        return http.build();
    }
}
