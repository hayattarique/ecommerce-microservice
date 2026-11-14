package org.ecommerce.auth.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the HttpOnly cookie that carries the refresh token (prefix auth.refresh-cookie).
 * The lifetime is not here: it is jwt.refresh-expiration, shared with the JWT and the DB row.
 *
 * @param name     cookie name. The "__Secure-" prefix makes browsers reject it unless secure=true.
 * @param path     the path the BROWSER sees, i.e. the gateway path, not the service path.
 *                 Limits the cookie to refresh-token and logout (and the other /auth calls).
 * @param secure   send over HTTPS only. Keep true; set false only if a local browser refuses it,
 *                 and then also drop the "__Secure-" prefix from the name.
 * @param sameSite Strict when frontend and gateway are on the same site, None when they are not.
 */
@ConfigurationProperties(prefix = "auth.refresh-cookie")
public record RefreshCookieProperties(
        @DefaultValue("__Secure-refresh") String name,
        @DefaultValue("/auth-service/api/v1/auth") String path,
        @DefaultValue("true") boolean secure,
        @DefaultValue("Strict") String sameSite) {
}
