package org.ecommerce.utility.security.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.ecommerce.utility.security.service.JwtTokenValidatorService;

import javax.crypto.SecretKey;

@RequiredArgsConstructor
public class JwtTokenValidatorServiceImpl implements JwtTokenValidatorService {

    private final SecretKey secretKey;

    @Override
    public Claims validateTokenAndGetClaims(String token) {

        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }
}
