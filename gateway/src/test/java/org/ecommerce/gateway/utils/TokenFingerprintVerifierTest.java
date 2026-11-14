package org.ecommerce.gateway.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.junit.jupiter.api.Assertions.*;

class TokenFingerprintVerifierTest {

    private static final String COOKIE = "__Secure-fgp";
    private final TokenFingerprintVerifier verifier = new TokenFingerprintVerifier(true, COOKIE);

    private static Claims claimsWithHashOf(String fingerprint) {
        return Jwts.claims().add("fgp", TokenFingerprintVerifier.sha256Hex(fingerprint)).build();
    }

    @Test
    void matching_cookie_passes() {
        var request = MockServerHttpRequest.get("/user-service/x").cookie(new HttpCookie(COOKIE, "fp-1")).build();
        assertTrue(verifier.matches(request, claimsWithHashOf("fp-1")));
    }

    @Test
    void missing_cookie_fails() {
        var request = MockServerHttpRequest.get("/user-service/x").build();
        assertFalse(verifier.matches(request, claimsWithHashOf("fp-1")));
    }

    @Test
    void cookie_from_another_login_fails() {
        var request = MockServerHttpRequest.get("/user-service/x").cookie(new HttpCookie(COOKIE, "fp-OTHER")).build();
        assertFalse(verifier.matches(request, claimsWithHashOf("fp-1")));
    }

    @Test
    void token_without_fgp_claim_fails() {
        var request = MockServerHttpRequest.get("/user-service/x").cookie(new HttpCookie(COOKIE, "fp-1")).build();
        assertFalse(verifier.matches(request, Jwts.claims().build()));
    }

    @Test
    void disabled_check_always_passes() {
        var request = MockServerHttpRequest.get("/user-service/x").build();
        assertTrue(new TokenFingerprintVerifier(false, COOKIE).matches(request, Jwts.claims().build()));
    }
}
