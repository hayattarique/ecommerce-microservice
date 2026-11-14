package org.ecommerce.auth.service.config;


import lombok.RequiredArgsConstructor;
import org.ecommerce.auth.service.entity.UserCredentialEntity;
import org.ecommerce.auth.service.integration.adapter.UserAdapter;
import org.ecommerce.auth.service.integration.dto.UserDto;
import org.ecommerce.auth.service.repositories.UserCredentialRepository;
import org.ecommerce.utility.security.model.AuthenticatedUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {
    private final UserCredentialRepository userCredentialRepository;
    private final UserAdapter userAdapter;

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            UserDto userDto = userAdapter.getUserByEmail(email);
            UserCredentialEntity userCredential = userCredentialRepository.findByUserAccountIdAndActiveIsTrue(userDto.getUserAccountId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return AuthenticatedUser.builder().email(email)
                    .id(userCredential.getId())
                    .userAccountId(userDto.getUserAccountId())
                    .email(userDto.getEmail())
                    .enabled(userCredential.isActive())
                    .password(userCredential.getPassword())
                    .roles(userDto.getRoles())
                    .permissions(userDto.getPermissions())
                    .build();
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}

