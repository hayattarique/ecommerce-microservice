package org.ecommerce.auth.service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.ecommerce.auth.service.dto.AuthenticationDto;
import org.ecommerce.auth.service.dto.AuthenticationRequest;
import org.ecommerce.auth.service.dto.SignupRequest;
import org.ecommerce.auth.service.dto.TokenResponse;
import org.ecommerce.auth.service.service.AuthenticationService;
import org.ecommerce.auth.service.service.RegistrationService;
import org.ecommerce.auth.service.util.RefreshCookieFactory;
import org.ecommerce.auth.service.util.TokenFingerprint;
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
    private final RefreshCookieFactory refreshCookieFactory;
    private final TokenFingerprint tokenFingerprint;


    @PostMapping(AuthMappingConstant.REGISTER)
    public ResponseEntity<ApiResponse<Object>> register(
            @Valid @RequestBody SignupRequest request) {

        log.info("User registration request received for email: {}", request.getEmail());

        Boolean result = registrationService.register(request);

        log.info("User registered successfully. Email: {}", request.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, ApiMessages.REGISTRATION_SUCCESS));
    }

    /** Access token in the body; refresh token and browser fingerprint in HttpOnly cookies. */
    @PostMapping(AuthMappingConstant.LOGIN)
    public ResponseEntity<ApiResponse<Object>> login(@Valid @RequestBody AuthenticationRequest request) {
        return withRefreshCookie(authenticationService.authenticate(request), ApiMessages.LOGIN_SUCCESS);
    }

    /** No body: the browser sends the refresh token as the cookie. The token is rotated. */
    @PostMapping(AuthMappingConstant.REFRESH_TOKEN)
    public ResponseEntity<ApiResponse<Object>> refreshToken(HttpServletRequest request) {
        return withRefreshCookie(authenticationService.refreshToken(refreshCookieFactory.read(request)),
                ApiMessages.OPERATION_SUCCESSFUL);
    }

    /**
     * No body and no access token needed: the cookie identifies the session, so logout still
     * works after the access token has expired. Both cookies are always cleared.
     */
    @PostMapping(AuthMappingConstant.LOGOUT)
    public ResponseEntity<ApiResponse<Object>> logout(HttpServletRequest request) {
        authenticationService.logout(refreshCookieFactory.read(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookieFactory.clear().toString(),
                        tokenFingerprint.clear().toString())
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(null, ApiMessages.LOGOUT_SUCCESS));
    }

    private ResponseEntity<ApiResponse<Object>> withRefreshCookie(AuthenticationDto tokens, String message) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookieFactory.build(tokens.getRefreshToken()).toString(),
                        tokenFingerprint.cookie(tokens.getFingerprint()).toString())
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(new TokenResponse(tokens.getId(), tokens.getToken()), message));
    }

}

