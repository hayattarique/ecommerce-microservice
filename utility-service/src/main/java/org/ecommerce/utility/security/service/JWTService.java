package org.ecommerce.utility.security.service;

import io.jsonwebtoken.Claims;
import org.ecommerce.utility.security.model.AuthUserDetails;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

public interface JWTService {


        String generateAccessToken(AuthUserDetails userDetails);

        String generateRefreshToken(AuthUserDetails userDetails);

        String extractUsername(String token);

        Long extractUserId(String token);

        String extractEmail(String token);

        List<String> extractRoles(String token);

        List<String> extractPermissions(String token);

        String extractTokenType(String token);

        Date extractExpiration(String token);

        boolean validateToken(String token);

        boolean isRefreshToken(String token);

        boolean isAccessToken(String token);

        <T> T extractClaim(String token,
                           Function<Claims, T> claimsResolver);
}
