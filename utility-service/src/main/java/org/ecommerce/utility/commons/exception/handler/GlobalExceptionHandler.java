package org.ecommerce.utility.commons.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.ecommerce.utility.commons.contract.ErrorCode;
import org.ecommerce.utility.commons.exception.BusinessException;
import org.ecommerce.utility.commons.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Error code for a request body that failed @Valid validation. */
    public static final String VALIDATION_FAILED = "VAL-001";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(new ErrorResponse(
                        errorCode.getHttpStatus().value(),
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        request.getRequestURI(),
                        LocalDateTime.now()
                ));
    }

    /**
     * A @Valid request body failed validation (e.g. gender "string" instead of MALE/FEMALE/OTHER).
     * Answers 400 with every field message, sorted by field, instead of letting Spring forward the
     * request to /error.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        VALIDATION_FAILED,
                        message.isEmpty() ? "Request validation failed." : message,
                        request.getRequestURI(),
                        LocalDateTime.now()
                ));
    }
}
