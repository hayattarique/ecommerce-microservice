package org.ecommerce.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The real user-service over HTTP (needs the local database, like UserServiceApplicationTests).
 * Before the fix both requests below came back as 401 "Unauthorized access to resource: GET /error".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrorResponseIntegrationTest {

    @Value("${local.server.port}")
    int port;

    private HttpResponse<String> register(String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/user/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void invalid_field_returns_400_with_the_field_message() throws Exception {
        // what Swagger sends by default: "string" for every text field
        HttpResponse<String> response = register(
                "{\"email\":\"a@b.com\",\"firstName\":\"A\",\"lastName\":\"B\",\"gender\":\"string\"}");

        assertEquals(400, response.statusCode(), response.body());
        assertTrue(response.body().contains("\"errorCode\":\"VAL-001\""), response.body());
        assertTrue(response.body().contains("gender: Gender must be MALE, FEMALE, or OTHER"), response.body());
    }

    @Test
    void other_errors_keep_their_real_status_instead_of_a_fake_401() throws Exception {
        // malformed JSON is not handled by GlobalExceptionHandler: Spring answers it through /error
        HttpResponse<String> response = register("{ this is not json");

        assertEquals(400, response.statusCode(), response.body());
    }
}
