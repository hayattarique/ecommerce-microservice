package org.ecommerce.auth.service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.utility.filter.JWTFilter;
import org.ecommerce.utility.service.JWTService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class JWTConfig {


    @Bean
    JWTFilter jwtFilter(JWTService jwtService) {
        return new JWTFilter(jwtService);
    }


}
