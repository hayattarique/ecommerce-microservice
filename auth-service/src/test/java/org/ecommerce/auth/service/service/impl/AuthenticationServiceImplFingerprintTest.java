package org.ecommerce.auth.service.service.impl;

import org.ecommerce.auth.service.dto.AuthenticationDto;
import org.ecommerce.auth.service.dto.AuthenticationRequest;
import org.ecommerce.auth.service.integration.adapter.UserAdapter;
import org.ecommerce.auth.service.service.TokenService;
import org.ecommerce.auth.service.util.TokenFingerprint;
import org.ecommerce.utility.security.model.AuthenticatedUser;
import org.ecommerce.utility.security.service.JwtTokenValidatorService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthenticationServiceImplFingerprintTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final TokenFingerprint tokenFingerprint = mock(TokenFingerprint.class);
    private final AuthenticationServiceImpl service = new AuthenticationServiceImpl(
            authenticationManager, tokenService, mock(JwtTokenValidatorService.class), mock(UserAdapter.class), tokenFingerprint);

    @Test
    void login_binds_the_access_token_to_a_new_fingerprint() {
        AuthenticatedUser user = AuthenticatedUser.builder().id(1L).userAccountId(42L).email("a@b.com").build();
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        when(tokenFingerprint.newFingerprint()).thenReturn("fp-raw");
        when(tokenService.generateAccessToken(eq(user), anyString())).thenReturn("access");
        when(tokenService.generateRefreshToken(user)).thenReturn("refresh");

        AuthenticationRequest request = new AuthenticationRequest();
        request.setEmail("a@b.com");
        request.setPassword("Secret@123");
        AuthenticationDto result = service.authenticate(request);

        // the raw value comes back for the cookie; the token received ONLY its SHA-256 hash
        assertEquals("fp-raw", result.getFingerprint());
        verify(tokenService).generateAccessToken(user, TokenFingerprint.hash("fp-raw"));
    }
}
