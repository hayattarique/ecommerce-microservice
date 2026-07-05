package org.ecommerce.auth.service.exception;

import lombok.Getter;
import org.ecommerce.utility.commons.contract.ErrorCode;
import org.ecommerce.utility.commons.exception.BusinessException;

@Getter
public class AuthenticationException extends BusinessException {


    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode);
    }
}