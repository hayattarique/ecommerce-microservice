package org.ecommerce.utility.security.exception;

import org.ecommerce.utility.commons.exception.BusinessException;

public class JwtException extends BusinessException {
    public JwtException(SecurityErrorCode errorCode) {
        super(errorCode);
    }
}
