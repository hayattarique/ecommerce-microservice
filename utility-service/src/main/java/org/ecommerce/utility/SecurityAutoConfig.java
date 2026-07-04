package org.ecommerce.utility;

import io.jsonwebtoken.security.Keys;
import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.ecommerce.utility.security.filter.JWTFilter;
import org.ecommerce.utility.security.service.JwtClaimExtractorService;
import org.ecommerce.utility.security.service.JwtTokenValidatorService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnClass(JWTFilter.class)
public class SecurityAutoConfig {

    @Bean
    SecretKey secretKey(JWTPropertiesConfig jwtPropertiesConfig) {
        return Keys.hmacShaKeyFor(jwtPropertiesConfig.getSecretKey()
                .getBytes(StandardCharsets.UTF_8));
    }

    ;

    @Bean
    @ConditionalOnMissingBean
    public JWTFilter jwtService(JwtTokenValidatorService tokenValidator, JwtClaimExtractorService claimExtractor) {
        return new JWTFilter(tokenValidator, claimExtractor);
    }
}
