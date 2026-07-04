package org.ecommerce.utility;

import org.ecommerce.utility.config.JWTPropertiesConfig;
import org.ecommerce.utility.service.JWTService;
import org.ecommerce.utility.service.impl.JWTServiceImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
@EnableConfigurationProperties(JWTPropertiesConfig.class)
public class SecurityAutoConfig {


    @Bean
    public JWTService jwtService(JWTPropertiesConfig jwtPropertiesConfig) {
        return new JWTServiceImpl(jwtPropertiesConfig);
    }
}
