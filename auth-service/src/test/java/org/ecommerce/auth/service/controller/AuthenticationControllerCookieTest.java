package org.ecommerce.auth.service.controller;

import jakarta.servlet.http.Cookie;
import org.ecommerce.auth.service.config.FingerprintCookieProperties;
import org.ecommerce.auth.service.config.RefreshCookieProperties;
import org.ecommerce.auth.service.dto.AuthenticationDto;
import org.ecommerce.auth.service.exception.AuthenticationException;
import org.ecommerce.auth.service.service.AuthenticationService;
import org.ecommerce.auth.service.service.RegistrationService;
import org.ecommerce.auth.service.util.AuthErrorCode;
import org.ecommerce.auth.service.util.RefreshCookieFactory;
import org.ecommerce.auth.service.util.TokenFingerprint;
import org.ecommerce.utility.commons.exception.handler.GlobalExceptionHandler;
import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The cookie contract of login, refresh and logout. Runs the real controller and cookie factory
 * with a mocked AuthenticationService - no database, no Eureka, no Spring context.
 */
class AuthenticationControllerCookieTest {

    private static final String COOKIE = "__Secure-refresh";
    private static final String FGP = "__Secure-fgp";
    private static final String BASE = "/api/v1/auth";

    private final AuthenticationService authenticationService = mock(AuthenticationService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        JWTPropertiesConfig jwt = new JWTPropertiesConfig("secret", 900_000L, 604_800_000L, "key");
        RefreshCookieFactory factory = new RefreshCookieFactory(
                new RefreshCookieProperties(COOKIE, "/auth-service/api/v1/auth", true, "Strict"), jwt);
        TokenFingerprint fingerprint = new TokenFingerprint(new FingerprintCookieProperties(FGP, true, "Strict"), jwt);
        AuthenticationController controller =
                new AuthenticationController(authenticationService, mock(RegistrationService.class), factory, fingerprint);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_puts_refresh_token_only_in_an_httponly_cookie() throws Exception {
        when(authenticationService.authenticate(any())).thenReturn(new AuthenticationDto(1L, "access-1", "refresh-1", "fp-1"));

        mvc.perform(post(BASE + "/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"Secret@123\"}"))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie", hasItems(
                        allOf(containsString(COOKIE + "=refresh-1"),
                                containsString("HttpOnly"),
                                containsString("Secure"),
                                containsString("SameSite=Strict"),
                                containsString("Path=/auth-service/api/v1/auth"),
                                containsString("Max-Age=604800")),
                        allOf(containsString(FGP + "=fp-1"),
                                containsString("HttpOnly"),
                                containsString("Secure"),
                                containsString("SameSite=Strict"),
                                containsString("Path=/;"),
                                containsString("Max-Age=604800")))))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.token").value("access-1"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.fingerprint").doesNotExist());
    }

    @Test
    void refresh_reads_the_cookie_and_rotates_it() throws Exception {
        when(authenticationService.refreshToken("refresh-1")).thenReturn(new AuthenticationDto(1L, "access-2", "refresh-2", "fp-2"));

        mvc.perform(post(BASE + "/refresh-token").cookie(new Cookie(COOKIE, "refresh-1")))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie", hasItems(
                        containsString(COOKIE + "=refresh-2"), containsString(FGP + "=fp-2"))))
                .andExpect(jsonPath("$.data.token").value("access-2"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    @Test
    void refresh_without_cookie_is_401() throws Exception {
        when(authenticationService.refreshToken(isNull()))
                .thenThrow(new AuthenticationException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        mvc.perform(post(BASE + "/refresh-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_301"));
    }

    @Test
    void logout_needs_no_access_token_and_clears_the_cookie() throws Exception {
        mvc.perform(post(BASE + "/logout").cookie(new Cookie(COOKIE, "refresh-1")))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie", hasItems(
                        allOf(containsString(COOKIE + "="), containsString("Max-Age=0"),
                                containsString("Path=/auth-service/api/v1/auth")),
                        allOf(containsString(FGP + "="), containsString("Max-Age=0"), containsString("Path=/;")))));

        verify(authenticationService).logout("refresh-1");
    }

    @Test
    void logout_without_cookie_still_succeeds_and_clears() throws Exception {
        mvc.perform(post(BASE + "/logout"))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie", hasItems(
                        allOf(containsString(COOKIE + "="), containsString("Max-Age=0")),
                        allOf(containsString(FGP + "="), containsString("Max-Age=0")))));

        verify(authenticationService).logout(null);
    }
}
