package org.ecommerce.auth.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the HttpOnly cookie that holds the browser fingerprint (prefix auth.fingerprint-cookie).
 * The path is always "/": the gateway must receive this cookie on EVERY API call, not only on /auth.
 * The lifetime is jwt.refresh-expiration; the cookie is replaced on every login and refresh.
 *
 * @param name     cookie name. Must equal gateway.token-fingerprint.cookie-name in the gateway.
 * @param secure   send over HTTPS only (localhost counts as secure in Chrome).
 * @param sameSite Strict when frontend and gateway are on the same site.
 */
@ConfigurationProperties(prefix = "auth.fingerprint-cookie")
public record FingerprintCookieProperties(
        @DefaultValue("__Secure-fgp") String name,
        @DefaultValue("true") boolean secure,
        @DefaultValue("Strict") String sameSite) {
}
