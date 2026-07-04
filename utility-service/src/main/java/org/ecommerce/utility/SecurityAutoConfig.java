package org.ecommerce.utility;

import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.ecommerce.utility.security.service.JWTService;
import org.ecommerce.utility.security.service.impl.JWTServiceImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JWTPropertiesConfig.class)
public class SecurityAutoConfig {


    @Bean
    public JWTService jwtService(JWTPropertiesConfig jwtPropertiesConfig) {
        return new JWTServiceImpl(jwtPropertiesConfig);
    }
}
