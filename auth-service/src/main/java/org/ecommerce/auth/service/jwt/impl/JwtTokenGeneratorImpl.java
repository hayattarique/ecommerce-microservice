package org.ecommerce.auth.service.jwt.impl;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.ecommerce.auth.service.jwt.JwtTokenGenerator;
import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.ecommerce.utility.security.model.AuthenticatedUser;
import org.ecommerce.utility.security.utils.TokenType;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.ecommerce.utility.security.constants.JwtClaimConstants.*;

@Component
@RequiredArgsConstructor
public class JwtTokenGeneratorImpl implements JwtTokenGenerator {
    private final JWTPropertiesConfig jwtProperties;
    private final SecretKey secretKey;

    @Override
    public String generateAccessToken(AuthenticatedUser user) {

        return buildToken(user, TokenType.ACCESS_TOKEN, jwtProperties.getExpiration());
    }

    @Override
    public String generateRefreshToken(AuthenticatedUser user) {

        return buildToken(user, TokenType.REFRESH_TOKEN,jwtProperties.getRefreshExpiration());
    }


    private String buildToken(AuthenticatedUser authenticatedUser, TokenType tokenType,long expiration) {
        return Jwts.builder()
                .subject(authenticatedUser.getEmail())
                .claim(USER_ID, authenticatedUser.getUserId())
                .claim(ROLES, authenticatedUser.getRoles())
                .claim(PERMISSIONS, authenticatedUser.getPermissions())
                .claim(TOKEN_TYPE, tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey).compact();
    }


}