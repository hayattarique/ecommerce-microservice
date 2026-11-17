package org.ecommerce.utility.commons.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/** The one JSON shape every error uses, in every service. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)   // hides errors when empty, traceId when missing
public class ErrorResponse {
    private int status;                 // always equal to the HTTP status
    private String errorCode;           // stable code from an ErrorCode enum
    private String message;             // safe text from the enum, never e.getMessage()
    private String path;
    private Map<String, String> errors; // only for validation: field -> message
    private String traceId;             // same id as in the logs
    private LocalDateTime timestamp;
}