package org.ecommerce.utility.service;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface JWTService {

    String extractUsername(String token);

    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

    String generateToken(UserDetails userDetails);

    String generateToken(Map<String, Object> extraClaims, UserDetails userDetails);

    String generateRefreshToken(UserDetails userDetails);

    String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration);


    boolean isTokenExpired(String token);

    List<String> extractRole(String token);

    boolean validateToken(String token);

}
