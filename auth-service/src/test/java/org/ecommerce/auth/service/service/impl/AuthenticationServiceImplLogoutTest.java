package org.ecommerce.auth.service.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.ecommerce.auth.service.exception.AuthenticationException;
import org.ecommerce.auth.service.integration.adapter.UserAdapter;
import org.ecommerce.auth.service.service.TokenService;
import org.ecommerce.auth.service.util.AuthErrorCode;
import org.ecommerce.utility.security.service.JwtTokenValidatorService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;

import static org.ecommerce.utility.security.constants.JwtClaimConstants.USER_ACCOUNT_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class AuthenticationServiceImplLogoutTest {

    private final TokenService tokenService = mock(TokenService.class);
    private final JwtTokenValidatorService validator = mock(JwtTokenValidatorService.class);
    private final AuthenticationServiceImpl service = new AuthenticationServiceImpl(
            mock(AuthenticationManager.class), tokenService, validator, mock(UserAdapter.class),
            mock(org.ecommerce.auth.service.util.TokenFingerprint.class));

    @Test
    void logout_takes_the_user_from_the_refresh_token_and_revokes() {
        Claims claims = Jwts.claims().add(USER_ACCOUNT_ID, 42L).build();
        when(tokenService.validateRefreshToken("refresh-1")).thenReturn(true);
        when(validator.validateTokenAndGetClaims("refresh-1")).thenReturn(claims);

        service.logout("refresh-1");

        verify(tokenService).revokeAllRefreshTokensForUser(42L);
    }

    @Test
    void logout_without_token_does_nothing() {
        service.logout(null);
        service.logout("  ");

        verifyNoInteractions(tokenService, validator);
    }

    @Test
    void logout_with_unknown_or_revoked_token_does_not_throw() {
        when(tokenService.validateRefreshToken("old"))
                .thenThrow(new AuthenticationException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        assertDoesNotThrow(() -> service.logout("old"));
        verify(tokenService, never()).revokeAllRefreshTokensForUser(anyLong());
    }

    @Test
    void logout_with_expired_token_does_not_throw() {
        when(tokenService.validateRefreshToken("expired"))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));

        assertDoesNotThrow(() -> service.logout("expired"));
        verify(tokenService, never()).revokeAllRefreshTokensForUser(anyLong());
    }

    @Test
    void refresh_without_token_is_rejected_before_any_lookup() {
        AuthenticationException e = assertThrows(AuthenticationException.class, () -> service.refreshToken(null));

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, e.getErrorCode());
        verifyNoInteractions(tokenService);
    }
}
