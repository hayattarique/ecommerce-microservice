package org.ecommerce.user.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ecommerce.utility.commons.contract.ErrorCode;
import org.springframework.http.HttpStatus;

/** Errors raised by user-service. GlobalExceptionHandler turns them into JSON. */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND("USER_404", "User not found.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
