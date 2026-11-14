package org.ecommerce.auth.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("auth.cookie")
public record CookiePropertiesConfig(
        @DefaultValue("__Secure-refresh") String name,
        @DefaultValue("/auth-service/api/v1/auth") String path,
        @DefaultValue("true") Boolean secure,
        @DefaultValue("Strict") String sameSite
) {
}
