package org.ecommerce.utility.commons.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.ecommerce.utility.commons.contract.ErrorCode;
import org.ecommerce.utility.commons.exception.BusinessException;
import org.ecommerce.utility.commons.exception.CommonErrorCode;
import org.ecommerce.utility.commons.util.ErrorResponse;
import org.ecommerce.utility.security.constants.AppConstant;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {


    /**
     * Custom handler to map Business exception
     *
     * @see BusinessException
     *
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusinessException(BusinessException be, HttpServletRequest request) {
        return business(be, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception e, HttpServletRequest request) throws Exception {
        // A business error wrapped by someone else (e.g. Spring Security wraps
        // "user-service is down" in InternalAuthenticationServiceException): keep its own status.
        BusinessException wrapped = findBusinessException(e);
        if (wrapped != null) {
            return business(wrapped, request);
        }
        // Not ours: throwing it again hands it back to Spring Security (401 / 403).
        if (e instanceof AccessDeniedException || e instanceof AuthenticationException) {
            throw e;
        }
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), e);
        return build(CommonErrorCode.INTERNAL_ERROR.getHttpStatus(), CommonErrorCode.INTERNAL_ERROR, null, request.getRequestURI());
    }

    /**
     * Override from super class
     * @see ResponseEntityExceptionHandler lot of pre-defines handlers are there
     *
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                            HttpStatusCode status, WebRequest request) {
        Map<String, String> errorMap = new TreeMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> errorMap.merge(error.getField(), Objects.requireNonNull(error.getDefaultMessage(), "is invalid"),
                (first, second) -> first + " : " + second));
        String path = pathOf(request);
        return build(status, CommonErrorCode.VALIDATION_FAILED, errorMap, path);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(@NonNull Exception ex, @Nullable Object body, @NonNull HttpHeaders headers,
                                                                       @NonNull HttpStatusCode statusCode, @NonNull WebRequest request) {

        if (request instanceof ServletWebRequest servletRequest) {
            HttpServletResponse response = servletRequest.getResponse();
            if (response != null && response.isCommitted()) {
                return null;
            }
        }
        String path = pathOf(request);
        if (statusCode.is5xxServerError()) {
            log.error("Request failed on {}", path, ex);
        } else {
            log.warn("Request rejected on {} with {}: {}", path, statusCode.value(), ex.getMessage());
        }
        return ResponseEntity.
                status(statusCode).
                headers(headers)
                .body(response(statusCode, CommonErrorCode.forStatus(statusCode), null, path))
                ;
    }

    /**
     * Helper method to find business exception inside nested exception class
     *
     * @param t contains cause of the exception
     * @return business exception
     */

    private static @Nullable BusinessException findBusinessException(Throwable t) {
        Throwable current = t.getCause();
        for (int i = 0; current != null && i < 10; i++) {

            if (current instanceof BusinessException businessException) {

                log.warn("Business Exception {}", businessException.getErrorCode().getMessage());
                return businessException;
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * Helper method to build business exception
     */
    private ResponseEntity<Object> business(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("Business error [{}] on {} {}", errorCode.getCode(), request.getMethod(), request.getRequestURI());
        return build(errorCode.getHttpStatus(), errorCode, null, request.getRequestURI());
    }

    private ResponseEntity<Object> build(HttpStatusCode statusCode, ErrorCode errorCode, @Nullable Map<String, String> errors, String path) {
        return ResponseEntity.status(statusCode).body(response(statusCode, errorCode, errors, path));
    }

    /**
     * Helper method to build error response
     */
    private ErrorResponse response(HttpStatusCode status, ErrorCode code, Map<String, String> errors, String path) {
        return ErrorResponse.builder().traceId(MDC.get(AppConstant.TRACE_ID)).
                timestamp(LocalDateTime.now()).errorCode(code.getCode()).status(status.value())
                .errors(errors).path(path).message(code.getMessage()).build();

    }

    private String pathOf(WebRequest request) {
        return request instanceof HttpServletRequest servlet ? servlet.getRequestURI() : request.getDescription(false);
    }
}