package org.ecommerce.gateway.utils;

import io.jsonwebtoken.Claims;
import org.ecommerce.gateway.constant.SecurityConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Checks that an access token is used from the browser it was issued to.
 *
 * auth-service puts SHA-256(fingerprint) in the token's "fgp" claim and the raw fingerprint in an
 * HttpOnly cookie. Here the cookie is hashed again and compared with the claim. JavaScript cannot read
 * an HttpOnly cookie, so an attacker who copies the token (e.g. via XSS) cannot copy the cookie with it.
 */
@Component
public class TokenFingerprintVerifier {

    private final boolean enabled;
    private final String cookieName;

    public TokenFingerprintVerifier(@Value("${gateway.token-fingerprint.enabled:true}") boolean enabled,
                                    @Value("${gateway.token-fingerprint.cookie-name:__Secure-fgp}") String cookieName) {
        this.enabled = enabled;
        this.cookieName = cookieName;
    }

    /** True when the request's fingerprint cookie matches the token's "fgp" claim (always true when disabled). */
    public boolean matches(ServerHttpRequest request, Claims claims) {
        if (!enabled) {
            return true;
        }
        String expectedHash = claims.get(SecurityConstants.FINGERPRINT_CLAIM, String.class);
        HttpCookie cookie = request.getCookies().getFirst(cookieName);
        if (expectedHash == null || cookie == null || cookie.getValue().isBlank()) {
            return false;
        }
        // constant-time comparison, so timing does not leak how many characters matched
        return MessageDigest.isEqual(
                sha256Hex(cookie.getValue()).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }

    static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in the JDK", e);
        }
    }
}
