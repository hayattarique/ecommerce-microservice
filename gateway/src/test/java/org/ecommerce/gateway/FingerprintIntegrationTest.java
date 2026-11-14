package org.ecommerce.gateway;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The real gateway with user-service replaced by a stub. Tokens are signed with the gateway's own
 * jwt.secret-key, exactly as auth-service signs them. Proves that a valid access token is accepted
 * ONLY together with its HttpOnly fingerprint cookie.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class FingerprintIntegrationTest {

    private static final String API = "/user-service/api/v1/users/me";
    private static final String COOKIE = "__Secure-fgp";

    static HttpServer userStub;
    static final AtomicInteger hits = new AtomicInteger();

    @DynamicPropertySource
    static void routeUserServiceToStub(DynamicPropertyRegistry registry) throws Exception {
        userStub = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        userStub.createContext("/", exchange -> {
            hits.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        userStub.start();
        String route = "spring.cloud.gateway.server.webflux.routes[0].";
        registry.add(route + "id", () -> "USER-SERVICE");
        registry.add(route + "uri", () -> "http://localhost:" + userStub.getAddress().getPort());
        registry.add(route + "predicates[0]", () -> "Path=/user-service/**");
        registry.add(route + "filters[0]", () -> "StripPrefix=1");
    }

    @AfterAll
    static void stopStub() {
        userStub.stop(0);
    }

    @Value("${local.server.port}")
    int port;

    @Value("${jwt.secret-key}")
    String secret;

    WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        hits.set(0);
    }

    /** An access token as auth-service issues it; fgpHash null = an old token without the claim. */
    private String accessToken(String fgpHash) throws Exception {
        var builder = Jwts.builder().subject("1")
                .claim("userAccountId", 42)
                .claim("tokenType", "ACCESS_TOKEN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000));
        if (fgpHash != null) {
            builder.claim("fgp", fgpHash);
        }
        return builder.signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void token_with_its_own_cookie_reaches_the_service() throws Exception {
        client.get().uri(API)
                .header("Authorization", "Bearer " + accessToken(sha256("fp-1")))
                .cookie(COOKIE, "fp-1")
                .exchange()
                .expectStatus().isOk();

        assertEquals(1, hits.get());
    }

    @Test
    void stolen_token_without_the_cookie_is_rejected() throws Exception {
        client.get().uri(API)
                .header("Authorization", "Bearer " + accessToken(sha256("fp-1")))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.errorCode").isEqualTo("SEC-006");

        assertEquals(0, hits.get());
    }

    @Test
    void token_with_another_browsers_cookie_is_rejected() throws Exception {
        client.get().uri(API)
                .header("Authorization", "Bearer " + accessToken(sha256("fp-1")))
                .cookie(COOKIE, "attackers-own-cookie")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.errorCode").isEqualTo("SEC-006");

        assertEquals(0, hits.get());
    }

    @Test
    void old_token_without_fgp_claim_is_rejected() throws Exception {
        client.get().uri(API)
                .header("Authorization", "Bearer " + accessToken(null))
                .cookie(COOKIE, "fp-1")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.errorCode").isEqualTo("SEC-006");
    }
}
