package org.ecommerce.auth.service.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.ecommerce.auth.service.config.RefreshCookieProperties;
import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

/**
 * Builds, reads and clears the refresh-token cookie, so every endpoint uses identical attributes.
 * Identical attributes matter: a browser only deletes a cookie when name, path and flags match.
 */
@Component
public class RefreshCookieFactory {

    private static final String SECURE_PREFIX = "__Secure-";

    private final RefreshCookieProperties properties;
    private final Duration maxAge;

    public RefreshCookieFactory(RefreshCookieProperties properties, JWTPropertiesConfig jwtProperties) {
        // Browsers silently drop these cookies - fail at startup instead of at the first login
        if (!properties.secure() && properties.name().startsWith(SECURE_PREFIX)) {
            throw new IllegalStateException("auth.refresh-cookie.name starts with " + SECURE_PREFIX
                    + " but auth.refresh-cookie.secure=false; browsers would reject the cookie");
        }
        if (!properties.secure() && "None".equalsIgnoreCase(properties.sameSite())) {
            throw new IllegalStateException("auth.refresh-cookie.same-site=None requires "
                    + "auth.refresh-cookie.secure=true; browsers would reject the cookie");
        }
        this.properties = properties;
        this.maxAge = Duration.ofMillis(jwtProperties.getRefreshExpiration());
    }

    /** The cookie to send after login or refresh. */
    public ResponseCookie build(String refreshToken) {
        return base(refreshToken).maxAge(maxAge).build();
    }

    /** The cookie to send on logout: same name, path and flags, empty value, Max-Age=0. */
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    /** The refresh token from the request, or null when the cookie is absent. */
    public String read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, properties.name());
        return cookie == null ? null : cookie.getValue();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(properties.name(), value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path(properties.path());
    }
}
