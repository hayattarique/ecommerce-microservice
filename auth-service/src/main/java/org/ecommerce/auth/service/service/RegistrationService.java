package org.ecommerce.auth.service.service;

import org.ecommerce.auth.service.dto.SignupRequest;

public interface RegistrationService {

    /**
     * Constructs credentials and registers the user with the user service.
     *
     * @param signupRequest the user registration data
     * @return the registered user details
     */
    Boolean register(SignupRequest signupRequest);
}
