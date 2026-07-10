package org.ecommerce.auth.service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.auth.service.dto.AuthenticationRequest;
import org.ecommerce.auth.service.dto.RefreshTokenRequest;
import org.ecommerce.auth.service.dto.SignupRequest;
import org.ecommerce.auth.service.service.AuthenticationService;
import org.ecommerce.auth.service.service.RegistrationService;
import org.ecommerce.auth.service.service.TokenService;
import org.ecommerce.utility.commons.constants.ApiMessages;
import org.ecommerce.utility.commons.constants.AuthMappingConstant;
import org.ecommerce.utility.commons.util.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping(AuthMappingConstant.BASE)
@Slf4j
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;
    private final TokenService tokenService;


    @PostMapping(AuthMappingConstant.REGISTER)
    public ResponseEntity<ApiResponse<Object>> register(
            @Valid @RequestBody SignupRequest request) {

        log.info("User registration request received for email: {}", request.getEmail());

        Boolean result = registrationService.register(request);

        log.info("User registered successfully. Email: {}", request.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, ApiMessages.REGISTRATION_SUCCESS));
    }

    @PostMapping(AuthMappingConstant.LOGIN)
    public ResponseEntity<ApiResponse<Object>> login(@Valid @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authenticationService.authenticate(request), ApiMessages.LOGIN_SUCCESS));
    }

    @PostMapping(AuthMappingConstant.REFRESH_TOKEN)
    public ResponseEntity<ApiResponse<Object>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authenticationService.refreshToken(request), ApiMessages.OPERATION_SUCCESSFUL));
    }

}

