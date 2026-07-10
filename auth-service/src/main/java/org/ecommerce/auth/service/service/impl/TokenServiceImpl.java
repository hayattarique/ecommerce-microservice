package org.ecommerce.auth.service.service.impl;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.auth.service.dto.AuthenticationDto;
import org.ecommerce.auth.service.entity.RefreshTokenEntity;
import org.ecommerce.auth.service.exception.AuthenticationException;
import org.ecommerce.auth.service.jwt.JwtTokenGenerator;
import org.ecommerce.auth.service.repositories.RefreshTokenRepository;
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

    @Override
    public String generateAccessToken(AuthenticatedUser user) {

        return jwtTokenGenerator.generateAccessToken(user);
    }

    @Override
    public String generateRefreshToken(AuthenticatedUser user) {
        return jwtTokenGenerator.generateRefreshToken(user);
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
        return jwtTokenGenerator.generateRefreshToken(user);
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


}
