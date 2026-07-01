package org.ecommerce.user.config;

import lombok.RequiredArgsConstructor;
import org.ecommerce.utility.filter.JWTFilter;
import org.ecommerce.utility.service.JWTService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    @Bean
    JWTFilter jwtFilter(JWTService jwtService) {
        return new JWTFilter(jwtService);
    }

}
