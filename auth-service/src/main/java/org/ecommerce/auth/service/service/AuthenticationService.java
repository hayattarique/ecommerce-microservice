package org.ecommerce.auth.service.service;

import org.ecommerce.auth.service.dto.AuthenticationRequest;
import org.ecommerce.auth.service.dto.AuthenticationDto;

public interface AuthenticationService {


    /**
     * Login user and generate access token and refresh token
     *
     * @param request the user login data
     * @return the authentication response containing access token and refresh token
     *
     */
    AuthenticationDto authenticate(AuthenticationRequest request);

    /**
     * Refresh user's access token using a valid refresh token
     *
     * @param refreshToken the refresh token from the cookie, or null when the cookie is absent
     * @return the updated authentication response containing new access and refresh tokens
     */
    AuthenticationDto refreshToken(String refreshToken);

    /**
     * Logout user: revoke the user's refresh tokens. Idempotent - a missing, expired or
     * already-revoked refresh token is not an error, there is simply nothing to revoke.
     *
     * @param refreshToken the refresh token from the cookie, or null when the cookie is absent
     */
    void logout(String refreshToken);
}
