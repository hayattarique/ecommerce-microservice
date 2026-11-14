package org.ecommerce.auth.service.util;

import org.ecommerce.auth.service.config.RefreshCookieProperties;
import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.servlet.http.Cookie;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RefreshCookieFactoryTest {

    private static final long SEVEN_DAYS_MS = 604_800_000L;

    private final JWTPropertiesConfig jwt = new JWTPropertiesConfig("secret", 3_600_000L, SEVEN_DAYS_MS, "key");
    private final RefreshCookieProperties props =
            new RefreshCookieProperties("__Secure-refresh", "/auth-service/api/v1/auth", true, "Strict");
    private final RefreshCookieFactory factory = new RefreshCookieFactory(props, jwt);

    @Test
    void build_sets_every_protective_attribute() {
        ResponseCookie cookie = factory.build("tok");

        assertEquals("__Secure-refresh", cookie.getName());
        assertEquals("tok", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals("/auth-service/api/v1/auth", cookie.getPath());
        assertEquals(Duration.ofMillis(SEVEN_DAYS_MS), cookie.getMaxAge());
    }

    @Test
    void clear_matches_name_path_and_flags_with_zero_max_age() {
        ResponseCookie built = factory.build("tok");
        ResponseCookie cleared = factory.clear();

        assertEquals(built.getName(), cleared.getName());
        assertEquals(built.getPath(), cleared.getPath());
        assertEquals(built.isSecure(), cleared.isSecure());
        assertEquals(built.getSameSite(), cleared.getSameSite());
        assertEquals("", cleared.getValue());
        assertEquals(Duration.ZERO, cleared.getMaxAge());
    }

    @Test
    void read_returns_the_cookie_value_or_null() {
        MockHttpServletRequest withCookie = new MockHttpServletRequest();
        withCookie.setCookies(new Cookie("__Secure-refresh", "tok"));

        assertEquals("tok", factory.read(withCookie));
        assertNull(factory.read(new MockHttpServletRequest()));
    }

    @Test
    void secure_prefix_without_secure_flag_fails_at_startup() {
        RefreshCookieProperties insecure =
                new RefreshCookieProperties("__Secure-refresh", "/auth-service/api/v1/auth", false, "Strict");

        assertThrows(IllegalStateException.class, () -> new RefreshCookieFactory(insecure, jwt));
    }

    @Test
    void same_site_none_without_secure_flag_fails_at_startup() {
        RefreshCookieProperties insecureNone =
                new RefreshCookieProperties("refresh", "/auth-service/api/v1/auth", false, "None");

        assertThrows(IllegalStateException.class, () -> new RefreshCookieFactory(insecureNone, jwt));
    }

    @Test
    void insecure_cookie_without_prefix_is_allowed_for_local_http() {
        RefreshCookieProperties local =
                new RefreshCookieProperties("refresh", "/auth-service/api/v1/auth", false, "Strict");

        assertDoesNotThrow(() -> new RefreshCookieFactory(local, jwt));
    }
}
