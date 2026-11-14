package org.ecommerce.auth.service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.ecommerce.auth.service.dto.*;
import org.ecommerce.auth.service.service.AuthenticationService;
import org.ecommerce.auth.service.service.RegistrationService;
import org.ecommerce.auth.service.util.CookieFactory;
import org.ecommerce.utility.commons.constants.ApiMessages;
import org.ecommerce.utility.commons.constants.AuthMappingConstant;
import org.ecommerce.utility.commons.util.ApiResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping(AuthMappingConstant.BASE)
@Log4j2
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;
    private final CookieFactory cookieFactory;


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
        return withRefreshCookie(authenticationService.authenticate(request),ApiMessages.LOGIN_SUCCESS);
    }

    @PostMapping(AuthMappingConstant.REFRESH_TOKEN)
    public ResponseEntity<ApiResponse<Object>> refreshToken(HttpServletRequest request) {

        String refreshToken = cookieFactory.readCookie(request);
        return withRefreshCookie(authenticationService.refreshToken(refreshToken),ApiMessages.OPERATION_SUCCESSFUL);
    }

    @PostMapping(AuthMappingConstant.LOGOUT)
    public ResponseEntity<ApiResponse<Object>> logout(HttpServletRequest request) {
        String token = cookieFactory.readCookie(request);
        authenticationService.logout(token);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookieFactory.clear().toString())
                .cacheControl(CacheControl.noCache())
                .body(ApiResponse.success(null,ApiMessages.LOGOUT_SUCCESS));
    }

    private ResponseEntity<ApiResponse<Object>> withRefreshCookie(AuthenticationDto dto,String message) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.build(dto.getRefreshToken()).toString())
                .cacheControl(CacheControl.noCache())
                .body(ApiResponse.success(new TokenResponse(dto.getId(), dto.getToken()), message));
    }

}

