package org.ecommerce.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the real gateway (no Eureka) with the auth-service route pointed at a stub server, and checks
 * the browser-facing rules: CORS for the frontend origin, rejection of foreign origins (the CSRF
 * guard for the refresh cookie), and removal of the Authorization header on the cookie endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "gateway.allowed-origins=http://localhost:3000"
})
class CorsAndOriginIntegrationTest {

    private static final String REFRESH = "/auth-service/api/v1/auth/refresh-token";
    private static final String LOGOUT = "/auth-service/api/v1/auth/logout";
    private static final String LOGIN = "/auth-service/api/v1/auth/login";

    /** Stands in for auth-service: records what reached it. */
    static HttpServer authStub;
    static final AtomicInteger hits = new AtomicInteger();
    static final AtomicReference<String> seenAuthorization = new AtomicReference<>();

    @DynamicPropertySource
    static void routeAuthServiceToStub(DynamicPropertyRegistry registry) throws Exception {
        authStub = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        authStub.createContext("/", exchange -> {
            hits.incrementAndGet();
            seenAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        authStub.start();
        // A list property is replaced as a whole by the highest source, so define the full route list here:
        // the same AUTH-SERVICE route as the profile files, pointed at the stub instead of lb://AUTH-SERVICE
        String route = "spring.cloud.gateway.server.webflux.routes[0].";
        registry.add(route + "id", () -> "AUTH-SERVICE");
        registry.add(route + "uri", () -> "http://localhost:" + authStub.getAddress().getPort());
        registry.add(route + "predicates[0]", () -> "Path=/auth-service/**");
        registry.add(route + "filters[0]", () -> "StripPrefix=1");
    }

    @AfterAll
    static void stopStub() {
        authStub.stop(0);
    }

    @Value("${local.server.port}")
    int port;

    WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        hits.set(0);
        seenAuthorization.set(null);
    }

    @Test
    void preflight_from_frontend_allows_credentials_for_that_exact_origin() {
        client.options().uri(REFRESH)
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000")
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");
    }

    @Test
    void preflight_from_unknown_origin_is_rejected() {
        client.options().uri(REFRESH)
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "POST")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist("Access-Control-Allow-Origin");
    }

    @Test
    void post_to_refresh_from_unknown_origin_never_reaches_auth_service() {
        // A browser still SENDS a simple cross-site POST with the cookie - the gateway CORS config must stop it
        client.post().uri(REFRESH)
                .header("Origin", "https://evil.example")
                .exchange()
                .expectStatus().isForbidden();

        assertEquals(0, hits.get());
    }

    @Test
    void same_origin_post_passes_e_g_swagger_ui_served_by_the_gateway() {
        client.post().uri(LOGOUT)
                .header("Origin", "http://localhost:" + port)
                .exchange()
                .expectStatus().isOk();

        assertEquals(1, hits.get());
    }

    @Test
    void frontend_can_read_the_trace_id_header() {
        client.post().uri(LOGIN)
                .header("Origin", "http://localhost:3000")
                .exchange()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000")
                .expectHeader().valueMatches("Access-Control-Expose-Headers", ".*X-Trace-Id.*");
    }

    @Test
    void authorization_header_is_removed_on_refresh_and_logout() {
        for (String path : new String[]{REFRESH, LOGOUT}) {
            seenAuthorization.set("not called");
            client.post().uri(path)
                    .header("Authorization", "Bearer expired-access-token")
                    .exchange()
                    .expectStatus().isOk();

            assertNull(seenAuthorization.get(), "Authorization must not reach auth-service on " + path);
        }
    }

    @Test
    void authorization_header_is_kept_on_other_endpoints() {
        client.post().uri(LOGIN)
                .header("Authorization", "Bearer some-token")
                .exchange()
                .expectStatus().isOk();

        assertEquals("Bearer some-token", seenAuthorization.get());
    }
}
