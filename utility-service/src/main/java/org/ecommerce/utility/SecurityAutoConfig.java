package org.ecommerce.utility;

import io.jsonwebtoken.security.Keys;
import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.ecommerce.utility.security.filter.JWTFilter;
import org.ecommerce.utility.security.filter.JwtAuthenticationEntryPoint;
import org.ecommerce.utility.security.service.JwtClaimExtractorService;
import org.ecommerce.utility.security.service.JwtTokenValidatorService;
import org.ecommerce.utility.security.service.impl.JwtClaimExtractorServiceImpl;
import org.ecommerce.utility.security.service.impl.JwtTokenValidatorServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;


@AutoConfiguration(before = UserDetailsServiceAutoConfiguration.class)
@ConditionalOnClass(SecurityFilterChain.class)
@EnableConfigurationProperties(JWTPropertiesConfig.class)
public class SecurityAutoConfig {


    @Bean
    @ConditionalOnMissingBean
    SecretKey secretKey(JWTPropertiesConfig jwtPropertiesConfig) {
        return Keys.hmacShaKeyFor(jwtPropertiesConfig.getSecretKey()
                .getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    @ConditionalOnMissingBean
    JwtTokenValidatorService jwtTokenValidatorService(
            SecretKey secretKey) {

        return new JwtTokenValidatorServiceImpl(secretKey);
    }

    @Bean
    @ConditionalOnMissingBean
    JwtClaimExtractorService jwtClaimExtractorService() {
        return new JwtClaimExtractorServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    JWTFilter jwtFilter(
            JwtTokenValidatorService validator,
            JwtClaimExtractorService extractor,
            JwtAuthenticationEntryPoint entryPoint) {

        return new JWTFilter(validator, extractor,entryPoint);
    }

    @Bean
    @ConditionalOnMissingBean
    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new JwtAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
    UserDetailsService jwtOnlyUserDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(
                    "No local user store -- this service authenticates by JWT only");
        };
    }
}
