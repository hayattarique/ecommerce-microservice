package org.ecommerce.gateway.utils;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ErrorResponse(int status, String errorCode, String message, String path,
                            LocalDateTime timestamp) implements Serializable {

    public ErrorResponse(int status, String errorCode, String message, String path) {
        this(status, errorCode, message, path, LocalDateTime.now());
    }


}
