package org.ecommerce.auth.service.service.impl;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.auth.service.entity.RefreshTokenEntity;
import org.ecommerce.auth.service.entity.UserCredentialEntity;
import org.ecommerce.auth.service.exception.AuthenticationException;
import org.ecommerce.auth.service.jwt.JwtTokenGenerator;
import org.ecommerce.auth.service.repositories.RefreshTokenRepository;
import org.ecommerce.auth.service.repositories.UserCredentialRepository;
import org.ecommerce.auth.service.service.TokenService;
import org.ecommerce.auth.service.util.AuthErrorCode;
import org.ecommerce.utility.security.constants.JwtClaimConstants;
import org.ecommerce.utility.security.model.AuthenticatedUser;
import org.ecommerce.utility.security.service.JwtTokenValidatorService;
import org.ecommerce.utility.security.utils.TokenType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenServiceImpl implements TokenService {

    //----------DEPENDENCIES-----------------------------------
    private final JwtTokenGenerator jwtTokenGenerator;
    private final JwtTokenValidatorService jwtTokenValidator;
    private final RefreshTokenRepository tokenRepository;
    private final UserCredentialRepository credentialRepository;

    @Override
    public String generateAccessToken(AuthenticatedUser user) {

        return jwtTokenGenerator.generateAccessToken(user);
    }

    @Override
    @Transactional
    public String generateRefreshToken(AuthenticatedUser user) {

        RefreshTokenEntity refreshTokenEntity = buildRefreshToken(user);

        return tokenRepository.save(refreshTokenEntity).getToken();
    }

    @Override
    public Boolean validateRefreshToken(String token) {

        tokenRepository.findByTokenAndActiveTrueAndExpiredAtAfter(token, LocalDateTime.now())
                .orElseThrow(() -> new AuthenticationException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        Claims claims = jwtTokenValidator.validateTokenAndGetClaims(token);
        TokenType tokenType = TokenType.valueOf(claims.get(JwtClaimConstants.TOKEN_TYPE, String.class));

        return tokenType == TokenType.REFRESH_TOKEN;
    }

    @Override
    @Transactional
    public String rotateRefreshToken(AuthenticatedUser user, String refreshToken) {
        if (refreshToken != null && validateRefreshToken(refreshToken)) {
            RefreshTokenEntity refreshTokenEntity = tokenRepository.findByTokenAndActiveTrueAndExpiredAtAfter(refreshToken, LocalDateTime.now())
                    .orElseThrow(() -> new AuthenticationException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));
            revoke(refreshTokenEntity);
            tokenRepository.save(refreshTokenEntity);
        }

        RefreshTokenEntity refreshTokenEntity = buildRefreshToken(user);
        return tokenRepository.save(refreshTokenEntity).getToken();
    }


    @Override
    @Transactional
    public void revokeAllRefreshTokensForUser(Long userId) {
        List<RefreshTokenEntity> refreshTokenEntities = tokenRepository.findAllByUserAccountIdAndActiveTrue(userId);
        refreshTokenEntities.forEach(this::revoke);
        tokenRepository.saveAll(refreshTokenEntities);
    }

    /**
     * Helper method to mark token entity inactive
     *
     * @param entity the refresh token entity to be revoked
     */
    private void revoke(RefreshTokenEntity entity) {
        entity.setActive(false);
        entity.setExpiredAt(LocalDateTime.now());
    }

    /**
     * Helper method to build a refresh token entity
     *
     * @param user the authenticated user
     * @return the built refresh token entity
     */

    private RefreshTokenEntity buildRefreshToken(AuthenticatedUser user) {
        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setUserAccountId(user.getUserAccountId());
        refreshTokenEntity.setToken(jwtTokenGenerator.generateRefreshToken(user));
        refreshTokenEntity.setExpiredAt(LocalDateTime.now().plusDays(7)); // Set expiration to 7 days from now
        // just fetch reference it won't fire query
        UserCredentialEntity reference = credentialRepository.getReferenceById(user.getId());
        refreshTokenEntity.setUserCredential(reference);
        return refreshTokenEntity;
    }


}
