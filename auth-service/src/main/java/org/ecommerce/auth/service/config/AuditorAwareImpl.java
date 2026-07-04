package org.ecommerce.auth.service.config;

import org.ecommerce.utility.security.model.AuthenticatedUser;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@NullMarked
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditorAwareImpl {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof AuthenticatedUser userDetails) {
                return Optional.of(userDetails.getUsername());
            }

            // handles "anonymousUser"
            return Optional.of("anonymous");
        };
    }
}
