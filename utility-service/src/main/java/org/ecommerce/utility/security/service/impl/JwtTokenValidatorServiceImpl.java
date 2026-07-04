package org.ecommerce.utility.security.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.ecommerce.utility.security.service.JwtTokenValidator;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class JwtTokenValidatorServiceImpl implements JwtTokenValidator {
    private final JWTPropertiesConfig  jwtPropertiesConfig;


    @Override
    public Claims validateTokenAndGetClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtPropertiesConfig.getSecretKey().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
