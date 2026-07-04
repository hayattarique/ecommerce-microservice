package org.ecommerce.auth.service.jwt;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.ecommerce.utility.security.constants.JwtClaimConstants;
import org.ecommerce.utility.security.model.AuthenticatedUser;
import org.ecommerce.utility.security.utils.TokenType;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtTokenGeneratorImpl implements JwtTokenGenerator {
    private final JWTPropertiesConfig jwtProperties;
    private final SecretKey secretKey;

    @Override
    public String generateAccessToken(AuthenticatedUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimConstants.USER_ID, user.getUserId());
        claims.put(JwtClaimConstants.EMAIL, user.getEmail());
        claims.put(JwtClaimConstants.ROLES, user.getRoles());
        claims.put(JwtClaimConstants.PERMISSIONS, user.getPermissions());
        claims.put(JwtClaimConstants.TOKEN_TYPE, TokenType.ACCESS_TOKEN.name());

        return buildToken(claims, jwtProperties.getExpiration());
    }

    @Override
    public String generateRefreshToken(AuthenticatedUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimConstants.USER_ID, user.getUserId());
        claims.put(JwtClaimConstants.EMAIL, user.getEmail());
        claims.put(JwtClaimConstants.TOKEN_TYPE, TokenType.REFRESH_TOKEN.name());
        return buildToken(claims, jwtProperties.getRefreshExpiration());
    }


    private String buildToken(Map<String, Object> claims, long expiration) {
        return Jwts.builder().claims(claims).subject(claims.get(JwtClaimConstants.EMAIL)
                        .toString()).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey).compact();
    }



}