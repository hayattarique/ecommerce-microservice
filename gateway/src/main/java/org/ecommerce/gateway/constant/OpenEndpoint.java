package org.ecommerce.gateway.constant;

import java.util.Set;

public interface OpenEndpoint {
    Set<String> openApi =
            Set.of("/auth-service/api/v1/auth/login",
                    "/auth-service/api/v1/auth/refresh-token",
                    "/auth-service/api/v1/auth/register",
                    // OpenAPI docs of each service, fetched by the gateway Swagger UI
                    "/auth-service/v3/api-docs",
                    "/user-service/v3/api-docs",
                    "/product-service/v3/api-docs");
}
