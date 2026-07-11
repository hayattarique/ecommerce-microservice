package org.ecommerce.utility.commons.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private int status;
    private String errorCode;
    private String message;
    private String path;
    private LocalDateTime timestamp;
}
