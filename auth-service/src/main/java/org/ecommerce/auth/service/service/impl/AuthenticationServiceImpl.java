package org.ecommerce.auth.service.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.ecommerce.auth.service.dto.AuthenticationDto;
import org.ecommerce.auth.service.dto.AuthenticationRequest;
import org.ecommerce.auth.service.exception.AuthenticationException;
import org.ecommerce.auth.service.integration.adapter.UserAdapter;
import org.ecommerce.auth.service.integration.dto.UserDto;
import org.ecommerce.auth.service.service.AuthenticationService;
import org.ecommerce.auth.service.service.TokenService;
import org.ecommerce.auth.service.util.AuthErrorCode;
import org.ecommerce.auth.service.util.TokenFingerprint;
import org.ecommerce.utility.security.model.AuthenticatedUser;
import org.ecommerce.utility.security.service.JwtTokenValidatorService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static org.ecommerce.utility.security.constants.JwtClaimConstants.EMAIL;
import static org.ecommerce.utility.security.constants.JwtClaimConstants.USER_ACCOUNT_ID;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthenticationServiceImpl implements AuthenticationService {

    //----------- DEPENDENCIES ---------------------------------------------------
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final JwtTokenValidatorService jwtTokenValidator;
    private final UserAdapter userAdapter;
    private final TokenFingerprint tokenFingerprint;


    @Override
    @Transactional
    public AuthenticationDto authenticate(AuthenticationRequest request) {
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        if (authenticate.getPrincipal() instanceof AuthenticatedUser authenticationDetails) {
            tokenService.revokeAllRefreshTokensForUser(authenticationDetails.getUserAccountId());
            // a new fingerprint per login: its hash goes into the access token, the raw value into a cookie
            String fingerprint = tokenFingerprint.newFingerprint();
            String accessToken = tokenService.generateAccessToken(authenticationDetails, TokenFingerprint.hash(fingerprint));
            String refreshToken = tokenService.generateRefreshToken(authenticationDetails);
            return new AuthenticationDto(authenticationDetails.getId(), accessToken, refreshToken, fingerprint);
        }
        throw new AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS);
    }

    @Override
    @Transactional
    public AuthenticationDto refreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new AuthenticationException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        // step 1: validate refresh token
        if (tokenService.validateRefreshToken(refreshToken)) {
            // step 2: extract claims
            Claims claims = jwtTokenValidator.validateTokenAndGetClaims(refreshToken);
            String email = claims.get(EMAIL, String.class);

            // step 3: get user details from user service
            UserDto user = userAdapter.getUserByEmail(email);

            // step 4: create authenticated user object
            AuthenticatedUser authenticatedUser = AuthenticatedUser.builder()
                    .id(Long.valueOf(claims.getSubject()))
                    .userAccountId(user.getUserAccountId())
                    .email(user.getEmail())
                    .roles(user.getRoles())
                    .permissions(user.getPermissions())
                    .build();

            // step 5: mark current refresh token invalid and generate new token
            String newRefreshToken = tokenService.rotateRefreshToken(authenticatedUser, refreshToken);

            // step 6: generate access token, bound to a NEW fingerprint (rotated together with the refresh token)
            String fingerprint = tokenFingerprint.newFingerprint();
            String accessToken = tokenService.generateAccessToken(authenticatedUser, TokenFingerprint.hash(fingerprint));
            return new AuthenticationDto(authenticatedUser.getId(), accessToken, newRefreshToken, fingerprint);
        }
        throw new AuthenticationException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Override
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            log.info("Logout without a refresh token - nothing to revoke");
            return;
        }
        try {
            if (tokenService.validateRefreshToken(refreshToken)) {
                // The user comes from the REFRESH token, so an expired access token cannot block logout
                Long userAccountId = jwtTokenValidator.validateTokenAndGetClaims(refreshToken)
                        .get(USER_ACCOUNT_ID, Long.class);
                tokenService.revokeAllRefreshTokensForUser(userAccountId);
            }
        } catch (AuthenticationException | JwtException | IllegalArgumentException e) {
            // Unknown, revoked or expired token: the session is already over
            log.info("Logout with an unusable refresh token - nothing to revoke: {}", e.getMessage());
        }
    }


}
