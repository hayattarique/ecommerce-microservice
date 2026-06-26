package org.ecommerce.utility.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JWTPropertiesConfig {
    private final String secretKey;
    private final Long expiration;
    private final Long refreshExpiration;
}
