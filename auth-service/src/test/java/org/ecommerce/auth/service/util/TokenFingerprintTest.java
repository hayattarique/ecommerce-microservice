package org.ecommerce.auth.service.util;

import org.ecommerce.auth.service.config.FingerprintCookieProperties;
import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TokenFingerprintTest {

    private final JWTPropertiesConfig jwt = new JWTPropertiesConfig("secret", 900_000L, 604_800_000L, "key");
    private final TokenFingerprint fingerprint =
            new TokenFingerprint(new FingerprintCookieProperties("__Secure-fgp", true, "Strict"), jwt);

    @Test
    void every_fingerprint_is_new_and_256_bits() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String fp = fingerprint.newFingerprint();
            assertEquals(43, fp.length(), "32 bytes in URL-safe Base64 without padding = 43 characters");
            assertTrue(seen.add(fp), "fingerprints must never repeat");
        }
    }

    @Test
    void hash_is_standard_sha256_hex() {
        // the published SHA-256 test vector for "abc"
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", TokenFingerprint.hash("abc"));
    }

    @Test
    void cookie_is_httponly_secure_strict_and_sent_on_every_path() {
        ResponseCookie cookie = fingerprint.cookie("fp-1");

        assertEquals("__Secure-fgp", cookie.getName());
        assertEquals("fp-1", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals("/", cookie.getPath());
        assertEquals(Duration.ofMillis(604_800_000L), cookie.getMaxAge());
    }

    @Test
    void clear_matches_the_cookie_with_zero_max_age() {
        ResponseCookie built = fingerprint.cookie("fp-1");
        ResponseCookie cleared = fingerprint.clear();

        assertEquals(built.getName(), cleared.getName());
        assertEquals(built.getPath(), cleared.getPath());
        assertEquals("", cleared.getValue());
        assertEquals(Duration.ZERO, cleared.getMaxAge());
    }

    @Test
    void invalid_combinations_fail_at_startup() {
        assertThrows(IllegalStateException.class, () -> new TokenFingerprint(
                new FingerprintCookieProperties("__Secure-fgp", false, "Strict"), jwt));
        assertThrows(IllegalStateException.class, () -> new TokenFingerprint(
                new FingerprintCookieProperties("fgp", false, "None"), jwt));
        assertDoesNotThrow(() -> new TokenFingerprint(new FingerprintCookieProperties("fgp", false, "Strict"), jwt));
    }
}
