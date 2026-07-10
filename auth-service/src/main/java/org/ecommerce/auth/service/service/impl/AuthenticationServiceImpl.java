package org.ecommerce.auth.service.service.impl;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.auth.service.dto.AuthenticationDto;
import org.ecommerce.auth.service.dto.AuthenticationRequest;
import org.ecommerce.auth.service.dto.RefreshTokenRequest;
import org.ecommerce.auth.service.exception.AuthenticationException;
import org.ecommerce.auth.service.integration.adapter.UserAdapter;
import org.ecommerce.auth.service.integration.dto.UserDto;
import org.ecommerce.auth.service.service.AuthenticationService;
import org.ecommerce.auth.service.service.TokenService;
import org.ecommerce.auth.service.util.AuthErrorCode;
import org.ecommerce.utility.security.model.AuthenticatedUser;
import org.ecommerce.utility.security.service.JwtTokenValidatorService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.ecommerce.utility.security.constants.JwtClaimConstants.EMAIL;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    //----------- DEPENDENCIES ---------------------------------------------------
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final JwtTokenValidatorService jwtTokenValidator;
    private final UserAdapter userAdapter;


    @Override
    @Transactional
    public AuthenticationDto authenticate(AuthenticationRequest request) {
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        if (authenticate.getPrincipal() instanceof AuthenticatedUser authenticationDetails) {
            tokenService.revokeAllRefreshTokensForUser(authenticationDetails.getUserId());
            String accessToken = tokenService.generateAccessToken(authenticationDetails);
            String refreshToken = tokenService.generateRefreshToken(authenticationDetails);
            return new AuthenticationDto(authenticationDetails.getUserId(), accessToken, refreshToken);
        }
        throw new AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS);
    }

    @Override
    @Transactional
    public AuthenticationDto refreshToken(RefreshTokenRequest request) {
        // step 1: validate refresh token
        if (tokenService.validateRefreshToken(request.refreshToken())) {
            // step 2: extract claims
            Claims claims = jwtTokenValidator.validateTokenAndGetClaims(request.refreshToken());
            String email = claims.get(EMAIL, String.class);

            // step 3: get user details from user service
            UserDto user = userAdapter.getUserByEmail(email);

            // step 4: create authenticated user object
            AuthenticatedUser authenticatedUser = AuthenticatedUser.builder()
                    .userId(user.getUserAccountId())
                    .email(user.getEmail())
                    .roles(user.getRoles())
                    .permissions(user.getPermissions())
                    .build();

            // step 5: mark current refresh token invalid and generate new token
            String refreshToken = tokenService.rotateRefreshToken(authenticatedUser, request.refreshToken());

            // step 6: generate access token
            String accessToken = tokenService.generateAccessToken(authenticatedUser);
            return new AuthenticationDto(authenticatedUser.getUserId(), accessToken, refreshToken);
        }
        throw new AuthenticationException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }


}
