package org.ecommerce.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties(prefix = "jwt")
@Configuration
public class JWTPropertiesConfig {
    private String secretKey;
    private Long expiration;
    private Long refreshExpiration;

}
