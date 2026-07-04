package org.ecommerce.utility.security.service.impl;

import io.jsonwebtoken.Claims;
import org.ecommerce.utility.security.model.AuthUserDetails;
import org.ecommerce.utility.security.service.JwtTokenExtractorService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JwtTokenExtractorServiceImpl implements JwtTokenExtractorService {
    @Override
    public AuthUserDetails extractUserDetailsFromToken(Claims claims) {


        return AuthUserDetails.builder()
                .userId(claims.get("userId",Long.class))
                .email(claims.get("email",String.class))
                .roles(extractRolesFromClaims(claims))
                .permissions(extractPermissionsFromClaims(claims))
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRolesFromClaims(Claims claims) {
        return claims.get("roles", List.class);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractPermissionsFromClaims(Claims claims) {
        return claims.get("permissions", List.class);
    }
}
