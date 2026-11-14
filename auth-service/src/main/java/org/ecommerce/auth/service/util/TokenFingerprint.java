package org.ecommerce.auth.service.util;

import org.ecommerce.auth.service.config.FingerprintCookieProperties;
import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Binds an access token to the browser that received it (OWASP "token sidejacking" defence).
 *
 * At login and refresh a random fingerprint is created. The raw value goes into an HttpOnly cookie
 * (JavaScript cannot read it); its SHA-256 hash goes into the access token's "fgp" claim. The gateway
 * accepts a token only when the cookie's hash equals the claim. A stolen access token is therefore
 * useless on any other machine: the thief has the token but not the cookie.
 */
@Component
public class TokenFingerprint {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SECURE_PREFIX = "__Secure-";

    private final FingerprintCookieProperties properties;
    private final Duration maxAge;

    public TokenFingerprint(FingerprintCookieProperties properties, JWTPropertiesConfig jwtProperties) {
        // Browsers silently drop these cookies - fail at startup instead of at the first login
        if (!properties.secure() && properties.name().startsWith(SECURE_PREFIX)) {
            throw new IllegalStateException("auth.fingerprint-cookie.name starts with " + SECURE_PREFIX
                    + " but auth.fingerprint-cookie.secure=false; browsers would reject the cookie");
        }
        if (!properties.secure() && "None".equalsIgnoreCase(properties.sameSite())) {
            throw new IllegalStateException("auth.fingerprint-cookie.same-site=None requires "
                    + "auth.fingerprint-cookie.secure=true; browsers would reject the cookie");
        }
        this.properties = properties;
        this.maxAge = Duration.ofMillis(jwtProperties.getRefreshExpiration());
    }

    /** A new random fingerprint: 32 random bytes (256 bits), URL-safe Base64. */
    public String newFingerprint() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** The value stored in the token: SHA-256 of the fingerprint, as lowercase hex. */
    public static String hash(String fingerprint) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(fingerprint.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in the JDK", e);
        }
    }

    /** The cookie to send after login or refresh. Path "/" so the gateway sees it on every call. */
    public ResponseCookie cookie(String fingerprint) {
        return base(fingerprint).maxAge(maxAge).build();
    }

    /** The cookie to send on logout: same name, path and flags, empty value, Max-Age=0. */
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(properties.name(), value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path("/");
    }
}
