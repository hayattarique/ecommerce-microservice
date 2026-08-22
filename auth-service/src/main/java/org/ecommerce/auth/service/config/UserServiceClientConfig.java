package org.ecommerce.auth.service.config;

import org.ecommerce.auth.service.integration.client.InternalClient;
import org.ecommerce.auth.service.integration.client.UserClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class UserServiceClientConfig {

    @Bean
    HttpServiceProxyFactory userServiceFactory(WebClient.Builder webClientBuilder) {

        WebClient build = webClientBuilder.baseUrl("http://USER-SERVICE").build();

        return HttpServiceProxyFactory.builderFor(WebClientAdapter.create(build)).build();
    }

    private <T> T createCient(HttpServiceProxyFactory factory, Class<T> clazz) {
        return factory.createClient(clazz);
    }

    @Bean
    UserClient userClient(HttpServiceProxyFactory factory) {
        return createCient(factory, UserClient.class);
    }

    @Bean
    InternalClient internalClient(HttpServiceProxyFactory factory) {
        return createCient(factory, InternalClient.class);
    }
}
