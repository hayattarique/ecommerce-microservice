package org.ecommerce.gateway.constant;

import java.util.Set;

public interface OpenEndpoint {
    Set<String> openApi =
            Set.of("/auth-service/api/v1/auth/login",
                    "/auth-service/api/v1/auth/refresh-token",
                    "/auth-service/api/v1/auth/register",
                    // The refresh cookie is logout's credential, so an expired access token must not block it
                    "/auth-service/api/v1/auth/logout",
                    // OpenAPI docs of each service, fetched by the gateway Swagger UI
                    "/auth-service/v3/api-docs",
                    "/user-service/v3/api-docs",
                    "/product-service/v3/api-docs");

    /**
     * Endpoints authenticated by the refresh-token COOKIE, not by a Bearer token.
     * CookieEndpointFilter removes any Authorization header on them (an expired access token
     * must not break refresh or logout). CSRF: the gateway CORS config rejects foreign origins.
     */
    Set<String> cookieAuthenticated =
            Set.of("/auth-service/api/v1/auth/refresh-token",
                    "/auth-service/api/v1/auth/logout");
}
