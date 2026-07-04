package org.ecommerce.utility.security.exception;

import org.ecommerce.utility.commons.contract.ErrorCode;
import org.ecommerce.utility.commons.exception.BusinessException;

public class JwtAuthenticationException extends BusinessException {
    public JwtAuthenticationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
