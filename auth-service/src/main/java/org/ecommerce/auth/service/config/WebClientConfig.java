package org.ecommerce.auth.service.config;

import org.ecommerce.auth.service.integration.client.UserClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClient() {
        return WebClient.builder();
    }

    @Bean
    public UserClient userClient( WebClient.Builder builder) {
        // Create WebClient with service discovery URL and timeout configurations
        WebClient webClient = builder.baseUrl("http://USER-SERVICE").build();


        // Create HTTP Service Proxy Factory
        // This factory creates a declarative HTTP client proxy for UserManagementClient interface

        return HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient))
                .build().createClient(UserClient.class);

    }


}
