package org.ecommerce.user.config;

import lombok.RequiredArgsConstructor;
import org.ecommerce.user.repositories.UserRoleRepository;
import org.ecommerce.utility.config.PermissionProvider;
import org.ecommerce.utility.filter.JWTFilter;
import org.ecommerce.utility.service.JWTService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {
    private final UserRoleRepository userRoleRepository;

    @Bean
    PermissionProvider permissionProvider() {
        return userRoleRepository::findAllPermissionByEmail;
    }

    @Bean
    JWTFilter jwtFilter(PermissionProvider permissionProvider, JWTService jwtService) {
        return new JWTFilter(jwtService, permissionProvider);
    }

}
