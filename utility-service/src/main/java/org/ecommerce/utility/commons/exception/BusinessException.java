package org.ecommerce.utility.commons.exception;

import lombok.Getter;
import org.ecommerce.utility.commons.contract.ErrorCode;

@Getter
public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
