package org.ecommerce.auth.service.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.auth.service.dto.AuthenticationRequest;
import org.ecommerce.auth.service.dto.AuthenticationResponse;
import org.ecommerce.auth.service.dto.SignupRequest;
import org.ecommerce.auth.service.entity.RefreshTokenEntity;
import org.ecommerce.auth.service.entity.UserCredentialEntity;
import org.ecommerce.auth.service.integration.adapter.UserAdapter;
import org.ecommerce.auth.service.integration.dto.UserDto;
import org.ecommerce.auth.service.repositories.RefreshTokenRepository;
import org.ecommerce.auth.service.repositories.UserCredentialRepository;
import org.ecommerce.auth.service.service.AuthenticationService;
import org.ecommerce.utility.model.AuthUserDetails;
import org.ecommerce.utility.service.JWTService;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    //----------- DEPENDENCIES ---------------------------------------------------
    private final UserCredentialRepository credentialRepository;
    private final UserDetailsService userDetailsService;
    private final HttpServletRequest httpServletRequest;
    private final PasswordEncoder passwordEncoder;
    private final UserAdapter userAdapter;
    private final JWTService jwtService;

    @Override
    @Transactional
    public Boolean register(@NonNull SignupRequest signupRequest) {
        log.info("Registering user with email: {}", signupRequest.getEmail());

        // Step 1: Validate password match first (before calling external service)
        validatePassword(signupRequest);

        try {
            // Step 2: Call UserAdapter to register with user service
            log.info("Calling UserAdapter to register user: {}", signupRequest.getEmail());
            UserDto userDto = userAdapter.register(signupRequest);

            // Step 3: Check if registration was successful
            if (userDto == null) {
                log.error("User registration failed - null response from UserAdapter");
                throw new IllegalStateException("Failed to register user in user service");
            }

            // Step 4: Save credentials in auth-service database
            log.info("Creating user credentials entity for userAccountId: {}", userDto.getUserAccountId());
            UserCredentialEntity credentialEntity =
                    new UserCredentialEntity();
            credentialEntity.setUserAccountId(userDto.getUserAccountId());
            credentialEntity.setPassword(signupRequest.getPassword()); // Already encoded by validatePassword()
            credentialEntity.setStatus(true);

            credentialRepository.save(credentialEntity);
            log.info("User credentials saved successfully for email: {}", signupRequest.getEmail());

            // Step 5: Return success
            return true;

        } catch (Exception e) {
            log.error("Error during user registration for email: {}", signupRequest.getEmail(), e);
            throw e; // Re-throw for GlobalExceptionHandler to catch
        }
    }

    @Override
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        UserDto user = userAdapter.getUserByEmail(request.getEmail());
        AuthUserDetails userDetails = (AuthUserDetails) userDetailsService.loadUserByUsername(request.getEmail());
        if (user == null) {
            log.error("User not found for email: {}", request.getEmail());
            throw new UsernameNotFoundException("User not found for email: " + request.getEmail());
        }
        UserCredentialEntity entity = credentialRepository.findByUserAccountIdAndActiveIsTrue(user.getUserAccountId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found for email: " + request.getEmail()));
        boolean isValid = passwordEncoder.matches(request.getPassword(), entity.getPassword());
        if (!isValid) {
            throw new BadCredentialsException("Invalid credentials");
        }
        String token = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        String ipAddress = getIpAddress(httpServletRequest);
        entity.setLastLoginIpAddress(ipAddress);
        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setExpiredAt(LocalDateTime.now().plusDays(7));
        refreshTokenEntity.setActive(true);
        refreshTokenEntity.setUserCredential(entity);
        entity.getRefreshTokens().add(refreshTokenEntity);
        credentialRepository.save(entity);
        return new AuthenticationResponse(token, refreshToken);
    }

    /**
     * Validate password and confirm password match
     * Encodes password if they match, otherwise throws exception
     */
    private void validatePassword(SignupRequest signupRequest) {
        if (!signupRequest.getPassword().equals(signupRequest.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        // Encode password for storage
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());
        signupRequest.setPassword(encodedPassword);
        log.debug("Password validation successful");
    }

    private String getIpAddress(HttpServletRequest request) {

        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // In case of multiple IPs, take first one
            return xForwardedFor.split(",")[0];
        }

        String proxyClientIp = request.getHeader("Proxy-Client-IP");
        if (proxyClientIp != null && !proxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(proxyClientIp)) {
            return proxyClientIp;
        }

        String wlProxyClientIp = request.getHeader("WL-Proxy-Client-IP");
        if (wlProxyClientIp != null && !wlProxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(wlProxyClientIp)) {
            return wlProxyClientIp;
        }

        return request.getRemoteAddr();
    }


}
