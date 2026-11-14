package org.ecommerce.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the real auth-service (full context, like AuthServiceApplicationTests, so it needs the local
 * database) and calls the cookie endpoints over HTTP, through Spring Security and the shared
 * GlobalExceptionHandler - nothing mocked.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthCookieEndpointsIntegrationTest {

    @Value("${local.server.port}")
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> post(String path, String cookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .POST(HttpRequest.BodyPublishers.noBody());
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void refresh_without_cookie_returns_the_real_error_code_not_a_generic_401() throws Exception {
        HttpResponse<String> response = post("/api/v1/auth/refresh-token", null);

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"errorCode\":\"AUTH_301\""), response.body());
        assertFalse(response.body().contains("/error"), "must be handled by GlobalExceptionHandler, not /error");
    }

    @Test
    void refresh_with_unknown_cookie_returns_its_error_code() throws Exception {
        HttpResponse<String> response = post("/api/v1/auth/refresh-token", "__Secure-refresh=not-a-real-token");

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"errorCode\":\"AUTH_302\""), response.body());
    }

    @Test
    void logout_with_unknown_cookie_succeeds_and_clears_it() throws Exception {
        HttpResponse<String> response = post("/api/v1/auth/logout", "__Secure-refresh=not-a-real-token");

        assertEquals(200, response.statusCode());
        String setCookie = response.headers().firstValue("Set-Cookie").orElse("");
        assertTrue(setCookie.contains("__Secure-refresh=") && setCookie.contains("Max-Age=0"), setCookie);
    }
}
