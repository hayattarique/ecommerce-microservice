package org.ecommerce.gateway.constant;

import java.util.List;

public interface OpenEndpoint {
    List<String> openApi = List.of("/auth-service/api/v1/auth/login", "/auth-service/api/v1/auth/register");
}
