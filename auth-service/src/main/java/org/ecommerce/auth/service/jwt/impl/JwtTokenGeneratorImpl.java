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
    public String generateAccessToken(AuthenticatedUser user, String fingerprintHash) {

        return buildToken(user, TokenType.ACCESS_TOKEN, jwtProperties.getExpiration(), fingerprintHash);
    }

    @Override
    public String generateRefreshToken(AuthenticatedUser user) {

        return buildToken(user, TokenType.REFRESH_TOKEN, jwtProperties.getRefreshExpiration(), null);
    }


    private String buildToken(AuthenticatedUser authenticatedUser, TokenType tokenType, long expiration, String fingerprintHash) {

        if (tokenType == TokenType.REFRESH_TOKEN) {
            return Jwts.builder()
                    .subject(authenticatedUser.getId().toString())
                    .claim(USER_ACCOUNT_ID, authenticatedUser.getUserAccountId())
                    .claim(EMAIL, authenticatedUser.getEmail())
                    .claim(TOKEN_TYPE, tokenType)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + expiration))
                    .signWith(secretKey).compact();
        }
        return Jwts.builder()
                .subject(authenticatedUser.getId().toString())
                .claim(USER_ACCOUNT_ID, authenticatedUser.getUserAccountId())
                .claim(EMAIL, authenticatedUser.getEmail())
                .claim(ROLES, authenticatedUser.getRoles())
                .claim(PERMISSIONS, authenticatedUser.getPermissions())
                .claim(TOKEN_TYPE, tokenType)
                // binds the access token to the browser's HttpOnly fingerprint cookie (checked by the gateway)
                .claim(FINGERPRINT_HASH, fingerprintHash)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey).compact();
    }


}