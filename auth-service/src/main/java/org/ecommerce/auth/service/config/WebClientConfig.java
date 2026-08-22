package org.ecommerce.auth.service.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClient(ObservationRegistry observationRegistry) {
        return WebClient.builder()
                .observationRegistry(observationRegistry)
                .filter((request, next) -> {
                    String token = getToken();// Ensure token is retrieved before proceeding
                    if (token != null && !token.isEmpty()) {
                        request = ClientRequest.from(request)
                                .headers(header->header.setBearerAuth(token))
                                .build();
                    }
                    return next.exchange(request);
                });
    }

    private String getToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getCredentials() instanceof String token) {
            return token;
        }
        return null;
    }


}
