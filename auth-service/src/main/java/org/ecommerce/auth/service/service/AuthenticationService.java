package org.ecommerce.auth.service.service;

import org.ecommerce.auth.service.dto.AuthenticationRequest;
import org.ecommerce.auth.service.dto.AuthenticationDto;
import org.ecommerce.auth.service.dto.RefreshTokenRequest;

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
     * @param request the refresh token request
     * @return the updated authentication response containing new access and refresh tokens
     */
    AuthenticationDto refreshToken(RefreshTokenRequest request);
}
