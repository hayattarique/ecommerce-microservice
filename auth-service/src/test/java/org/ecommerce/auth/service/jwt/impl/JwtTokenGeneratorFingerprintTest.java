package org.ecommerce.auth.service.jwt.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.ecommerce.utility.security.model.AuthenticatedUser;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.ecommerce.utility.security.constants.JwtClaimConstants.FINGERPRINT_HASH;
import static org.junit.jupiter.api.Assertions.*;

class JwtTokenGeneratorFingerprintTest {

    private final SecretKey key = Keys.hmacShaKeyFor("a-test-secret-that-is-at-least-32-bytes!".getBytes(StandardCharsets.UTF_8));
    private final JwtTokenGeneratorImpl generator = new JwtTokenGeneratorImpl(
            new JWTPropertiesConfig("unused", 900_000L, 604_800_000L, "key"), key);
    private final AuthenticatedUser user = AuthenticatedUser.builder().id(1L).userAccountId(42L).email("a@b.com").build();

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    @Test
    void access_token_carries_the_fingerprint_hash() {
        Claims claims = claims(generator.generateAccessToken(user, "abc123hash"));

        assertEquals("abc123hash", claims.get(FINGERPRINT_HASH, String.class));
    }

    @Test
    void access_token_lives_15_minutes() {
        Claims claims = claims(generator.generateAccessToken(user, "h"));
        long lifetimeMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertEquals(900_000L, lifetimeMs, 1_000L);
    }

    @Test
    void refresh_token_has_no_fingerprint_claim() {
        Claims claims = claims(generator.generateRefreshToken(user));

        assertNull(claims.get(FINGERPRINT_HASH));
    }
}
