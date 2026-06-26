package org.ecommerce.auth.service.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

    private final String errorCode;
    private final int statusCode;

    public ServiceException(String message, String errorCode, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }



}