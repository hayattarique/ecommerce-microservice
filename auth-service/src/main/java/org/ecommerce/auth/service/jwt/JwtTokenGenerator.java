package org.ecommerce.auth.service.jwt;

import org.ecommerce.utility.security.model.AuthenticatedUser;

public interface JwtTokenGenerator {
    /**
     * Generates a JWT token for the given authenticated user.
     *
     * @param user the authenticated user
     * @return the generated JWT token
     */
    String generateAccessToken(AuthenticatedUser user);

    /**
     * Generates a refresh JWT token for the given authenticated user.
     *
     * @param user the authenticated user
     * @return the generated refresh JWT token
     */
    String generateRefreshToken(AuthenticatedUser user);
}
