package org.ecommerce.auth.service.exception;

import org.ecommerce.utility.commons.contract.ErrorCode;
import org.ecommerce.utility.commons.exception.BusinessException;

public class DownstreamServiceException extends BusinessException {
    public DownstreamServiceException(ErrorCode errorCode) {
        super(errorCode);
    }
}
