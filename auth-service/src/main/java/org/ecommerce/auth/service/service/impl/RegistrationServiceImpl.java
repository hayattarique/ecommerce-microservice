package org.ecommerce.auth.service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.auth.service.dto.SignupRequest;
import org.ecommerce.auth.service.entity.UserCredentialEntity;
import org.ecommerce.auth.service.exception.AuthenticationException;
import org.ecommerce.auth.service.exception.DownstreamServiceException;
import org.ecommerce.auth.service.integration.adapter.UserAdapter;
import org.ecommerce.auth.service.integration.dto.UserDto;
import org.ecommerce.auth.service.repositories.UserCredentialRepository;
import org.ecommerce.auth.service.service.RegistrationService;
import org.ecommerce.auth.service.util.AuthErrorCode;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {

    //----------- DEPENDENCIES ---------------------------------------------------
    private final UserAdapter userAdapter;
    private final UserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Boolean register(@NonNull SignupRequest signupRequest) {
        log.info("Registering user with email: {}", signupRequest.getEmail());

        // Step 1: Validate password match first (before calling external service)
        String encodedPassword = validatePasswordAndReturn(signupRequest);

        try {
            // Step 2: Call UserAdapter to register with user service
            log.info("Calling UserAdapter to register user: {}", signupRequest.getEmail());
            UserDto userDto = userAdapter.register(signupRequest);

            // Step 3: Check if registration was successful
            if (userDto == null) {
                log.error("User registration failed - null response from UserAdapter");
                throw new DownstreamServiceException(AuthErrorCode.REGISTRATION_FAILED);
            }

            // Step 4: Save credentials in auth-service database
            log.info("Creating user credentials entity for userAccountId: {}", userDto.getUserAccountId());
            UserCredentialEntity credentialEntity =
                    new UserCredentialEntity();
            credentialEntity.setUserAccountId(userDto.getUserAccountId());
            credentialEntity.setPassword(encodedPassword); // Already encoded by validatePassword()
            credentialEntity.setStatus(true);

            credentialRepository.save(credentialEntity);
            log.info("User credentials saved successfully for email: {}", signupRequest.getEmail());

            // Step 5: Return success
            return true;

        } catch (AuthenticationException e) {
            log.error("Error during user registration for email: {}", signupRequest.getEmail(), e);
            throw new DownstreamServiceException(AuthErrorCode.REGISTRATION_FAILED); // Re-throw for GlobalExceptionHandler to catch
        }
    }

    /**
     * Validate password and confirm password match
     * Encodes password if they match, otherwise throws exception
     */
    private String validatePasswordAndReturn(SignupRequest signupRequest) {
        if (!signupRequest.getPassword().equals(signupRequest.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        // Encode password for storage
        log.debug("Password validation successful");
        return passwordEncoder.encode(signupRequest.getPassword());
    }
}
