package org.ecommerce.auth.service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.auth.service.integration.client.UserClient;
import org.ecommerce.utility.config.PermissionProvider;
import org.ecommerce.utility.filter.JWTFilter;
import org.ecommerce.utility.service.JWTService;
import org.ecommerce.utility.util.ApiResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class JWTConfig {
    private final UserClient userManagementClient;

    @Bean
    PermissionProvider permissionProvider() {
        return username -> {
            log.info("Getting permissions for user {}", username);
            ResponseEntity<ApiResponse<List<String>>> permissions = userManagementClient.findPermissionsByUsername(username);
            if (permissions.getStatusCode().is2xxSuccessful() && permissions.getBody() != null) {
                log.info("Found permissions for user {}", username);
                return permissions.getBody().getData();
            }
            log.warn("Permissions not found for user {}", username);
            return List.of();
        };
    }

    @Bean
    JWTFilter jwtFilter(JWTService jwtService,PermissionProvider permissionProvider) {
        return new JWTFilter(jwtService,permissionProvider);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}
