package org.ecommerce.utility.security.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ecommerce.utility.commons.contract.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SecurityErrorCode implements ErrorCode {

    INVALID_TOKEN(
            "SEC-001",
            "Invalid JWT token",
            HttpStatus.UNAUTHORIZED
    ),

    TOKEN_EXPIRED(
            "SEC-002",
            "JWT token expired",
            HttpStatus.UNAUTHORIZED
    ),

    INVALID_TOKEN_TYPE(
            "SEC-003",
            "Invalid token type",
            HttpStatus.UNAUTHORIZED
    ),
    INVALID_API_KEY(
            "SEC-004",
            "Invalid api key"
            , HttpStatus.UNAUTHORIZED
    );

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;


}