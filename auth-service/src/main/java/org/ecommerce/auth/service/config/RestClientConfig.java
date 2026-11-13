package org.ecommerce.auth.service.config;

import lombok.extern.log4j.Log4j2;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.ecommerce.auth.service.exception.DownstreamServiceException;
import org.ecommerce.auth.service.integration.client.InternalClient;
import org.ecommerce.auth.service.integration.client.UserClient;
import org.ecommerce.auth.service.util.AuthErrorCode;
import org.ecommerce.utility.commons.constants.SecurityConstants;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * How auth-service calls user-service.
 * Part 1: @ImportHttpServices turns the UserClient and InternalClient interfaces into working clients.
 * Part 2: a connection pool, so calls reuse connections and many logins can run at once.
 * Part 3: three rules applied to every call of every service group (unreachable, error status, forward the token).
 *
 * Do NOT set connect or read timeouts here. They come from spring.http.clients.* and
 * spring.http.serviceclient.user-service.* in application.properties.
 */
@Log4j2
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "user-service", types = {InternalClient.class, UserClient.class})
public class RestClientConfig {

    // ---------- Part 2: connection pool ----------
    @Bean
    ClientHttpRequestFactoryBuilder<?> pooledRequestFactoryBuilder() {
        return ClientHttpRequestFactoryBuilder.httpComponents()
                // Apache default is only 5 per host and 25 in total, so raise it.
                .withConnectionManagerCustomizer(pool -> pool.setMaxConnTotal(200).setMaxConnPerRoute(50))
                // Do not keep a connection longer than 5 minutes (instances come and go).
                // Test a connection that was idle for 10 s before reusing it.
                .withConnectionConfigCustomizer(c -> c
                        .setTimeToLive(TimeValue.ofMinutes(5))
                        .setValidateAfterInactivity(TimeValue.ofSeconds(10)))
                // If the pool is full, fail after 2 s instead of waiting forever.
                .withDefaultRequestConfigCustomizer(c -> c.setConnectionRequestTimeout(Timeout.ofSeconds(2)))
                // Clean up expired and idle connections in the background.
                .withHttpClientCustomizer(c -> c.evictExpiredConnections().evictIdleConnections(TimeValue.ofSeconds(30)));
    }

    // ---------- Part 3: rules for every call ----------
    @Bean
    RestClientHttpServiceGroupConfigurer outboundCallConfigurer() {
        return new RestClientHttpServiceGroupConfigurer() {
            @Override
            public void configureGroups(Groups<RestClient.Builder> groups) {
                groups.forEachClient((group, builder) -> builder
                        .requestInterceptor(whenUnreachable(group.name()))
                        .requestInterceptor(forwardToken())
                        .defaultStatusHandler(HttpStatusCode::isError, whenErrorStatus(group.name())));
            }

            @Override
            public int getOrder() {
                return 5;   // must run before Spring Cloud's load-balancer configurer (order 10)
            }
        };
    }

    /** The called service did not answer at all (down, refused, timeout, network problem) -> DOWNSTREAM_UNAVAILABLE. */
    private ClientHttpRequestInterceptor whenUnreachable(String service) {
        return (request, body, execution) -> {
            try {
                return execution.execute(request, body);
            } catch (IOException | IllegalStateException e) {
                log.error("DOWNSTREAM {} UNREACHABLE - {} {}", service, request.getMethod(), request.getURI(), e);
                throw new DownstreamServiceException(AuthErrorCode.DOWNSTREAM_UNAVAILABLE);
            }
        };
    }

    /**
     * The called service answered with 4xx or 5xx.
     * 5xx or 429 -> its problem, the caller may retry later (DOWNSTREAM_COMMUNICATION_FAILED).
     * any other 4xx -> our request was wrong, retrying will not help (DOWNSTREAM_REQUEST_REJECTED).
     */
    private RestClient.ResponseSpec.ErrorHandler whenErrorStatus(String service) {
        return (request, response) -> {
            HttpStatusCode status = response.getStatusCode();
            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            log.error("DOWNSTREAM {} FAILED - {} {} - status={} body={}",
                    service, request.getMethod(), request.getURI(), status, body);

            boolean retryable = status.is5xxServerError() || status.value() == 429;
            throw new DownstreamServiceException(
                    retryable ? AuthErrorCode.DOWNSTREAM_COMMUNICATION_FAILED : AuthErrorCode.DOWNSTREAM_REQUEST_REJECTED,
                    status);
        };
    }

    /** Send the caller's JWT to the called service, unless the call already sets its own Authorization header. */
    private ClientHttpRequestInterceptor forwardToken() {
        return (request, body, execution) -> {
            if (!request.getHeaders().containsHeader(SecurityConstants.AUTHORIZATION_HEADER)) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && auth.getCredentials() instanceof String token && !token.isBlank()) {
                    request.getHeaders().setBearerAuth(token);
                }
            }
            return execution.execute(request, body);
        };
    }
}
