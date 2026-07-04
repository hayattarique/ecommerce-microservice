package org.ecommerce.utility.security.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.ecommerce.utility.security.model.AuthUserDetails;
import org.ecommerce.utility.security.service.JWTService;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;

@RequiredArgsConstructor
public class JWTServiceImpl implements JWTService {

    private final JWTPropertiesConfig jwtProperties;

    @Override
    public String generateAccessToken(AuthUserDetails userDetails) {

        Map<String, Object> claims = new HashMap<>();

        claims.put("userId", userDetails.getUserId());
        claims.put("email", userDetails.getEmail());
        claims.put("roles", userDetails.getRoles());
        claims.put("permissions", userDetails.getPermissions());
        claims.put("type", "access");

        return buildToken(
                claims,
                userDetails.getUsername(),
                jwtProperties.getExpiration()
        );
    }

    @Override
    public String generateRefreshToken(AuthUserDetails userDetails) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        return buildToken(
                claims,
                userDetails.getUsername(),
                jwtProperties.getRefreshExpiration()
        );
    }

    private String buildToken(
            Map<String, Object> claims,
            String username,
            long expiration) {

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    @Override
    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        List<?> roles = extractAllClaims(token).get("roles", List.class);
        return roles == null
                ? Collections.emptyList()
                : roles.stream().map(Object::toString).toList();
    }

    @Override
    public List<String> extractPermissions(String token) {
        List<?> permissions = extractAllClaims(token).get("permissions", List.class);
        return permissions == null
                ? Collections.emptyList()
                : permissions.stream().map(Object::toString).toList();
    }

    @Override
    public String extractTokenType(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

    @Override
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @Override
    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractTokenType(token));
    }

    @Override
    public boolean isAccessToken(String token) {
        return "access".equals(extractTokenType(token));
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public <T> T extractClaim(String token,
                              Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {

        byte[] keyBytes =
                Decoders.BASE64.decode(jwtProperties.getSecretKey());

        return Keys.hmacShaKeyFor(keyBytes);
    }
}