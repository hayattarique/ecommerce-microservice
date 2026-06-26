package org.ecommerce.auth.service.service;

import org.ecommerce.auth.service.dto.AuthenticationRequest;
import org.ecommerce.auth.service.dto.AuthenticationResponse;
import org.ecommerce.auth.service.dto.SignupRequest;

public interface AuthenticationService {

    /**
     * Constructs credentials and registers the user with the user service.
     *
     * @param signupRequest the user registration data
     * @return the registered user details
     */
    Boolean register(SignupRequest signupRequest);


    /**
     * Login user and generate access token and refresh token
     *
     * @param request the user login data
     * @return the authentication response containing access token and refresh token
     *
     */
    AuthenticationResponse authenticate(AuthenticationRequest request);


}
