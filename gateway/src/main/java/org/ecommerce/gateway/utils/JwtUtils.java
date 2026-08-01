package org.ecommerce.gateway.utils;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.ecommerce.gateway.config.JWTPropertiesConfig;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JWTPropertiesConfig jwtProperties;

    /**
     * Verifies the signature and expiry of the token and returns its claims.
     * <p>
     * Callers should read individual claims from the returned object rather than calling back
     * in per claim — every call here re-verifies the HMAC, which is the expensive part.
     *
     * @throws io.jsonwebtoken.ExpiredJwtException if the token has expired
     * @throws io.jsonwebtoken.JwtException        if the signature or format is invalid
     * @throws IllegalArgumentException            if the token is null, empty, or blank
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }
}
