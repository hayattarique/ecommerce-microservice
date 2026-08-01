package org.ecommerce.utility.security.exception;

import org.ecommerce.utility.commons.contract.ErrorCode;
import org.springframework.security.core.AuthenticationException;

public class JwtException extends AuthenticationException {
    private final ErrorCode errorCode;

    public JwtException(SecurityErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
