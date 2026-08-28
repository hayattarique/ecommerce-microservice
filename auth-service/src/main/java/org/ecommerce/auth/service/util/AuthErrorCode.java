package org.ecommerce.auth.service.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ecommerce.utility.commons.contract.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {


    REGISTRATION_FAILED(
      "AUTH_202",
      "User registration failed.",
      HttpStatus.CONFLICT
    ),

    INVALID_CREDENTIALS(
            "AUTH_101",
            "Invalid email or password.",
            HttpStatus.UNAUTHORIZED
    ),

    USER_ALREADY_EXISTS(
            "AUTH_201",
            "User already exists.",
            HttpStatus.CONFLICT
    ),

    INVALID_REFRESH_TOKEN(
            "AUTH_301",
            "Refresh token is invalid.",
            HttpStatus.UNAUTHORIZED
    ),
    REFRESH_TOKEN_NOT_FOUND(
            "AUTH_302",
            "Refresh token not found.",
            HttpStatus.NOT_FOUND
    ),

    USER_SERVICE_UNAVAILABLE(
            "AUTH_501",
            "User service is temporarily unavailable.",
            HttpStatus.SERVICE_UNAVAILABLE
    ),

    USER_SERVICE_COMMUNICATION_FAILED(
            "AUTH_502",
            "Unable to communicate with user service.",
            HttpStatus.SERVICE_UNAVAILABLE
    ),

    /**
     * The downstream answered with 4xx - it understood us and refused.
     *
     * A 4xx means the request WE sent was wrong: bad payload, missing header, expired
     * internal key. That is our defect, and repeating the same request can never fix it -
     * so this maps to 500, not 503. Answering 503 here would tell the caller "retry later"
     * and invite a retry storm on a request that is guaranteed to fail every time.
     */
    DOWNSTREAM_REQUEST_REJECTED(
            "AUTH_503",
            "Internal server error.",
            HttpStatus.INTERNAL_SERVER_ERROR
    ),

    INTERNAL_SERVER_ERROR(
            "AUTH_901",
            "Internal server error.",
            HttpStatus.INTERNAL_SERVER_ERROR
    );

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    
}
