package org.ecommerce.auth.service.exception;

import org.ecommerce.utility.commons.contract.ErrorCode;
import org.ecommerce.utility.commons.exception.BusinessException;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatusCode;

public class DownstreamServiceException extends BusinessException {

    private final @Nullable HttpStatusCode downstreamStatus;

    public DownstreamServiceException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public DownstreamServiceException(ErrorCode errorCode, @Nullable HttpStatusCode downstreamStatus) {
        super(errorCode);
        this.downstreamStatus = downstreamStatus;
    }

    /** The downstream's status code, or {@code null} if no HTTP response was received. */
    public @Nullable HttpStatusCode getDownstreamStatus() {
        return downstreamStatus;
    }

    /** True when the downstream replied with exactly this status. Null-safe. */
    public boolean isStatus(HttpStatusCode status) {
        return status.equals(downstreamStatus);
    }
}
