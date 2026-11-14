package org.ecommerce.utility;

import org.ecommerce.utility.commons.exception.handler.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

/**
 * Registers the shared GlobalExceptionHandler in every servlet service that depends on utility-service.
 *
 * Without it, the services only scan their own packages, so the handler is never loaded: a
 * BusinessException then falls through to Spring's /error page, which Spring Security blocks,
 * and the client receives a generic 401 "Authentication required" instead of the real error code.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(GlobalExceptionHandler.class)
public class ExceptionHandlerAutoConfig {
}
