package org.ecommerce.auth.service.config;

import lombok.extern.log4j.Log4j2;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.ecommerce.auth.service.integration.client.InternalClient;
import org.ecommerce.auth.service.integration.client.UserClient;
import org.ecommerce.auth.service.exception.DownstreamServiceException;
import org.ecommerce.auth.service.util.AuthErrorCode;
import org.ecommerce.utility.commons.constants.SecurityConstants;
import org.ecommerce.utility.commons.contract.ErrorCode;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Log4j2
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "user-service", types = {InternalClient.class, UserClient.class})
public class RestClientConfig {

    /**
     * One Apache HttpClient 5 connection pool for the whole service.
     * This bean replaces Boot's own builder, so every group is built on this same pool.
     * maxConnTotal              -> maximum HTTP connections across the whole application.
     * maxConnPerRoute           -> maximum connections to one particular service/host.
     * connectionTimeToLive      -> how long a connection may stay alive/reusable.
     * validateAfterInactivity   -> validates a long-idle connection before reusing it.
     * connectionRequestTimeout  -> how long to wait when no pooled connection is free.
     * evictExpiredConnections() -> removes expired connections from the pool.
     * evictIdleConnections()    -> removes connections unused for a long time.
     * <p>
     * DO NOT SET CONNECT / READ TIMEOUT HERE.
     * Boot passes those in from spring.http.clients.* when it calls build(settings) on
     * this builder. Adding setConnectTimeout or setResponseTimeout here would overwrite
     * Boot's value, and then a single timeout would stick to every group - meaning
     * spring.http.serviceclient.GROUP.read-timeout would never take effect.
     * Measured: with setResponseTimeout present, a per-group 1s property was ignored.
     */
    @Bean
    ClientHttpRequestFactoryBuilder<?> pooledRequestFactoryBuilder(RestClientProperties properties) {

        return ClientHttpRequestFactoryBuilder.httpComponents()

                .withConnectionManagerCustomizer(connectionManager ->
                        connectionManager.setMaxConnPerRoute(properties.getMaxConnectionPerRoute())
                                .setMaxConnTotal(properties.getMaxConnection()))

                .withConnectionConfigCustomizer(connectionConfig ->
                        connectionConfig.setTimeToLive(TimeValue.ofSeconds(properties.getConnectionTimeToLive()))
                                .setValidateAfterInactivity(TimeValue.ofSeconds(properties.getValidateAfterInactive())))

                .withDefaultRequestConfigCustomizer(requestConfig ->
                        requestConfig.setConnectionRequestTimeout(
                                Timeout.ofSeconds(properties.getConnectionRequestTimeout())))

                .withHttpClientCustomizer(httpClient ->
                        httpClient.evictExpiredConnections()
                                .evictIdleConnections(TimeValue.ofSeconds(properties.getIdleEvictionThreshold())));
    }


    @Bean
    RestClientHttpServiceGroupConfigurer outboundCallConfigurer() {
        return new RestClientHttpServiceGroupConfigurer() {

            @Override
            public void configureGroups(Groups<RestClient.Builder> groups) {
                groups.forEachClient((group, builder) -> builder
                        .requestInterceptor(translateTransportFailures(group.name()))
                        .requestInterceptor(forwardCallerToken())
                        .defaultStatusHandler(HttpStatusCode::isError, translateErrorStatus(group.name())));
            }

            @Override
            public int getOrder() {
                return 5;   // before Spring Cloud's load-balancer configurer (10)
            }
        };
    }


    private RestClient.ResponseSpec.ErrorHandler translateErrorStatus(String serviceName) {
        return (request, response) -> {
            HttpStatusCode status = response.getStatusCode();
            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);

            log.error("DOWNSTREAM {} FAILED - {} {} - status={} body={}",
                    serviceName, request.getMethod(), request.getURI(), status, body);

            /*
             * Whose fault it was decides what our own caller is told, and that decides whether
             * they retry. Mapping everything to 503 - as this used to - tells a caller "try
             * again later" even when we sent a malformed request that will fail identically
             * every time.
             */
            ErrorCode errorCode = worthRetrying(status)
                    ? AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED   // 503 - their problem
                    : AuthErrorCode.DOWNSTREAM_REQUEST_REJECTED;        // 500 - our problem

            throw new DownstreamServiceException(errorCode, status);
        };
    }

    /**
     * Would sending the exact same request again have a chance of succeeding?
     *
     * <ul>
     *   <li>5xx - the downstream broke. It may well be fine on the next attempt.</li>
     *   <li>429 - it is explicitly telling us to slow down, not that the request is wrong.</li>
     *   <li>every other 4xx - the request itself is the problem. Retrying is pointless.</li>
     * </ul>
     */
    private static boolean worthRetrying(HttpStatusCode status) {
        return status.is5xxServerError() || status.value() == 429;
    }


    private ClientHttpRequestInterceptor translateTransportFailures(String serviceName) {
        return (request, body, execution) -> {
            try {
                return execution.execute(request, body);
            } catch (IOException | IllegalStateException e) {
                log.error("DOWNSTREAM {} UNREACHABLE - {} {}",
                        serviceName, request.getMethod(), request.getURI(), e);
                throw new DownstreamServiceException(AuthErrorCode.USER_SERVICE_UNAVAILABLE);
            }
        };
    }

    private ClientHttpRequestInterceptor forwardCallerToken() {
        return (request, body, execution) -> {
            boolean alreadyPresent = request.getHeaders().containsHeader(SecurityConstants.AUTHORIZATION_HEADER);
            if (!alreadyPresent) {
                String token = token();
                if (StringUtils.hasText(token)) {
                    request.getHeaders().setBearerAuth(token);
                }
            }
            return execution.execute(request, body);
        };
    }

    private String token() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getCredentials() instanceof String token) {
            return token;
        }
        return null;
    }
}
