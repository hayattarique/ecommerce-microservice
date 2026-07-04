package org.ecommerce.utility.security.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.ecommerce.utility.security.constants.JwtClaimConstants;
import org.ecommerce.utility.security.service.JwtTokenValidatorService;
import org.ecommerce.utility.security.utils.TokenType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
@RequiredArgsConstructor
public class JwtTokenValidatorServiceImpl implements JwtTokenValidatorService {

    private final SecretKey secretKey;

    @Override
    public Claims validateTokenAndGetClaims(String token) {

        Claims payload = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        if (!payload.get(JwtClaimConstants.TOKEN_TYPE).equals(TokenType.ACCESS_TOKEN.name())) {
            throw new BadCredentialsException("Invalid token type");
        }
        return payload;
    }
}
