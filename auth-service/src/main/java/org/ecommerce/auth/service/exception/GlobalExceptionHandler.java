package org.ecommerce.auth.service.exception;

import lombok.extern.slf4j.Slf4j;
import org.ecommerce.utility.commons.constants.ApiMessages;
import org.ecommerce.utility.commons.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

/**
 * Global exception handler for handling all exceptions across the application
 * Centralizes error handling and provides consistent API error responses
 * Uses ErrorResponse for detailed error information including status, error type, path, and timestamp
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ========== MOST SPECIFIC EXCEPTIONS FIRST ==========

    /**
     * Handle ServiceException (custom REST exception)
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceException e, WebRequest request) {
        log.error("Service error: {}", e.getMessage());
        String path = getPath(request);
        ErrorResponse errorResponse = new ErrorResponse(
                e.getStatusCode(),
                e.getErrorCode(),
                e.getMessage(),
                path,
                LocalDateTime.now()
        );
        return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
    }

    /**
     * Handle validation errors from Bean Validation (@Valid)
     * Spring automatically throws MethodArgumentNotValidException when @Valid fails
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        log.warn("Validation failed with {} errors", ex.getBindingResult().getErrorCount());
        
        // Extract first field error message for response
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(msg -> msg != null && !msg.isEmpty())
                .findFirst()
                .orElse(ApiMessages.INVALID_REQUEST);
        
        String path = getPath(request);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                errorMessage,
                path,
                LocalDateTime.now()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle RestClientException (external service failures)
     * Place this BEFORE RuntimeException since RestClientException extends RuntimeException
     */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> handleRestClientException(
            RestClientException ex,
            WebRequest request) {
        
        log.error("External service error: {}", ex.getMessage(), ex);
        
        String path = getPath(request);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                ApiMessages.SERVICE_UNAVAILABLE,
                path,
                LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    /**
     * Handle IllegalArgumentException (validation logic errors)
     * Place this BEFORE RuntimeException since IllegalArgumentException extends RuntimeException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {
        
        log.warn("Illegal argument: {}", ex.getMessage());
        
        String path = getPath(request);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Argument",
                ex.getMessage(),
                path,
                LocalDateTime.now()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle IllegalStateException (business logic conflicts)
     * Place this BEFORE RuntimeException since IllegalStateException extends RuntimeException
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex,
            WebRequest request) {
        
        log.warn("State error: {}", ex.getMessage());
        
        String path = getPath(request);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "State Conflict",
                ex.getMessage(),
                path,
                LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    // ========== GENERIC EXCEPTIONS LAST ==========

    /**
     * Handle RuntimeException (parent of IllegalArgumentException, IllegalStateException, etc.)
     * MUST come AFTER specific RuntimeException subclasses
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            WebRequest request) {
        
        log.error("Runtime error: {}", ex.getMessage(), ex);
        
        String path = getPath(request);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Runtime Error",
                ex.getMessage(),
                path,
                LocalDateTime.now()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle all other unexpected exceptions
     * This is the catch-all handler - MUST come LAST
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {
        
        log.error("Unexpected error occurred", ex);
        
        String path = getPath(request);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ApiMessages.INTERNAL_ERROR,
                path,
                LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Extract path from WebRequest
     */
    private String getPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return "Unknown";
    }
}
