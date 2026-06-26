package org.ecommerce.utility.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.ecommerce.utility.config.JWTPropertiesConfig;
import org.ecommerce.utility.service.JWTService;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;

@RequiredArgsConstructor
public class JWTServiceImpl implements JWTService {

    private final JWTPropertiesConfig jwtProperties;

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claim = extractAllClaims(token);
        return claimsResolver.apply(claim);

    }

    @Override
    public String generateToken(UserDetails userDetails) {

        Map<String, Object> claims = new HashMap<>();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(auth -> Objects.requireNonNull(auth.getAuthority()).replace("ROLE_", ""))
                .toList();
        claims.put("roles", roles);

        return buildToken(claims, userDetails, jwtProperties.getExpiration());
    }

    @Override
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtProperties.getExpiration());
    }

    @Override
    public String generateRefreshToken(UserDetails userDetails) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        return buildToken(claims, userDetails, jwtProperties.getRefreshExpiration());
    }

    @Override
    public String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder().claims(extraClaims).subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis())).expiration(new Date(System.currentTimeMillis() + expiration)).signWith(getSignInKey()).compact();
    }


    @Override
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    @Override
    public List<String> extractRole(String token) {
        return  extractAllClaims(token).get("role", List.class);
    }


    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
