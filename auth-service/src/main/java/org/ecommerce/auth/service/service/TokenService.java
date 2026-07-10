package org.ecommerce.auth.service.service;

import org.ecommerce.utility.security.model.AuthenticatedUser;

public interface TokenService {

    /**
     * Generates an access token for the authenticated user.
     *
     * @param user the authenticated user
     * @return the generated access token
     */

    String generateAccessToken(AuthenticatedUser user);

    /**
     * Generates a refresh token for the specified user.
     *
     * @param user the authenticated user
     * @return the generated refresh token
     */
    String generateRefreshToken(AuthenticatedUser user);

    /**
    * Validates the given refresh token.
    *
    * @param token the refresh token to validate
    * @return true if the token is valid, false otherwise
    */

    Boolean validateRefreshToken(String token);

    /**
     * Generates a refresh token for the specified user and invalidate current token.
     *
     * @param user the authenticated user
     * @return the generated refresh token
     */
    String rotateRefreshToken(AuthenticatedUser user, String refreshToken);


    /**
     * Revokes all refresh tokens for the specified user.
     *
     * @param userId the ID of the user whose refresh tokens to revoke
     */

    void revokeAllRefreshTokensForUser(Long userId);


}
