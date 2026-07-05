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

    INTERNAL_SERVER_ERROR(
            "AUTH_901",
            "Internal server error.",
            HttpStatus.INTERNAL_SERVER_ERROR
    );

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
