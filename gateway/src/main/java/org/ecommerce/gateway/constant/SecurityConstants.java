package org.ecommerce.gateway.constant;

public interface SecurityConstants {
    String AUTHORIZATION_HEADER = "Authorization";
    String AUTHORIZATION_HEADER_PREFIX = "Bearer ";

    // JWT claim names — must match org.ecommerce.utility.security.constants.JwtClaimConstants
    String TOKEN_TYPE = "tokenType";
    String USER_ACCOUNT_ID_CLAIM = "userAccountId";
    String ACCESS_TOKEN = "ACCESS_TOKEN";
    // SHA-256 (hex) of the browser fingerprint cookie, put in the access token by auth-service
    String FINGERPRINT_CLAIM = "fgp";

    // header the gateway adds for downstream services
    String USER_ACCOUNT_ID_HEADER = "X-Forward-User-Account-Id";
}
