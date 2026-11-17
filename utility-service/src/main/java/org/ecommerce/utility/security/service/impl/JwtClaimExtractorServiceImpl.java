package org.ecommerce.utility.security.service.impl;

import io.jsonwebtoken.Claims;
import org.ecommerce.utility.security.constants.AppConstant;
import org.ecommerce.utility.security.model.AuthenticatedUser;
import org.ecommerce.utility.security.service.JwtClaimExtractorService;

import java.util.List;

public class JwtClaimExtractorServiceImpl implements JwtClaimExtractorService {
    @Override
    public AuthenticatedUser extractAuthenticatedUser(Claims claims) {


        return AuthenticatedUser.builder()
                .id(Long.valueOf(claims.getSubject()))
                .userAccountId(claims.get(AppConstant.USER_ACCOUNT_ID, Long.class))
                .email(claims.get(AppConstant.EMAIL, String.class))
                .roles(extractRolesFromClaims(claims))
                .permissions(extractPermissionsFromClaims(claims))
                .accountNonLocked(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .enabled(true)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRolesFromClaims(Claims claims) {
        return claims.get(AppConstant.ROLES, List.class);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractPermissionsFromClaims(Claims claims) {
        return claims.get(AppConstant.PERMISSIONS, List.class);
    }
}
