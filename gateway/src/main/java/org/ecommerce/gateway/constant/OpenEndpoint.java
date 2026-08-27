package org.ecommerce.gateway.constant;

import java.util.Set;

public interface OpenEndpoint {
    Set<String> openApi =
            Set.of("/auth-service/api/v1/auth/login",
                    "/auth-service/api/v1/auth/refresh-token",
                    "/auth-service/api/v1/auth/register");
}
