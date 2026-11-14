package org.ecommerce.utility.commons.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.ecommerce.utility.commons.contract.ErrorCode;
import org.ecommerce.utility.commons.exception.BusinessException;
import org.ecommerce.utility.commons.util.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
}
