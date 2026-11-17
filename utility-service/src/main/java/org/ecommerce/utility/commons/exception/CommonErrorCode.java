package org.ecommerce.utility.commons.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ecommerce.utility.commons.contract.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Errors every service can raise, used by {@link org.ecommerce.utility.commons.exception.handler.GlobalExceptionHandler}.
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    VALIDATION_FAILED("COM-400", "Validation failed.", HttpStatus.BAD_REQUEST),
    BAD_REQUEST("COM-401", "The request could not be processed.", HttpStatus.BAD_REQUEST),
    NOT_FOUND("COM-404", "The requested resource was not found.", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("COM-405", "This HTTP method is not allowed here.", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE("COM-415", "This content type is not supported.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    INTERNAL_ERROR("COM-500", "Something went wrong. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;


    public static CommonErrorCode forStatus(HttpStatusCode statusCode) {

        if (statusCode.is5xxServerError()) {
            return INTERNAL_ERROR;
        }
        return switch (statusCode.value()) {
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            default -> BAD_REQUEST;
        };
    }
}
