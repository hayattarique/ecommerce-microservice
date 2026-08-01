package org.ecommerce.gateway.utils;

public enum GatewayErrorCode {
    INVALID_TOKEN("SEC-001", "Invalid JWT token"),
    TOKEN_EXPIRED("SEC-002", "JWT token expired"),
    INVALID_TOKEN_TYPE("SEC-003", "Invalid token type"),
    MISSING_TOKEN("SEC-005", "Authentication required");

    private final String code;
    private final String message;

    GatewayErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

   public String code() {
        return code;
    }

   public String message() {
        return message;
    }
}
