package org.ecommerce.utility.security.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.ecommerce.utility.commons.constants.SecurityConstants;
import org.ecommerce.utility.security.constants.JwtClaimConstants;
import org.ecommerce.utility.security.exception.JwtException;
import org.ecommerce.utility.security.exception.SecurityErrorCode;
import org.ecommerce.utility.security.model.AuthenticatedUser;
import org.ecommerce.utility.security.service.JwtClaimExtractorService;
import org.ecommerce.utility.security.service.JwtTokenValidatorService;
import org.ecommerce.utility.security.utils.TokenType;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Log4j2
public class JWTFilter extends OncePerRequestFilter {

    private final JwtTokenValidatorService jwtTokenValidator;
    private final JwtClaimExtractorService jwtClaimExtractor;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        //step 1 check auth header is there and start with Bearer
        if (header == null || !header.startsWith(SecurityConstants.AUTHORIZATION_HEADER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(SecurityConstants.AUTHORIZATION_HEADER_PREFIX.length());
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            // step 2 validate token
            Claims claims;
            try {
                claims = jwtTokenValidator.validateTokenAndGetClaims(token);
            } catch (ExpiredJwtException e) {
                reject(request, response, SecurityErrorCode.TOKEN_EXPIRED);
                return;
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
                reject(request, response, SecurityErrorCode.INVALID_TOKEN);
                return;
            }
            if (TokenType.REFRESH_TOKEN.name().equals(claims.get(JwtClaimConstants.TOKEN_TYPE, String.class))) {
                reject(request, response, SecurityErrorCode.INVALID_TOKEN_TYPE);
                return;
            }
            String userAccountId = request.getHeader(JwtClaimConstants.USER_ACCOUNT_ID_HEADER);

            // step 3 extract userId from claims and set it in MDC for logging
            if (userAccountId!= null) {
                MDC.put(JwtClaimConstants.USER_ACCOUNT_ID, userAccountId);
            }
            AuthenticatedUser authenticatedUser = jwtClaimExtractor.extractAuthenticatedUser(claims);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(authenticatedUser, token, authenticatedUser.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        }
        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, SecurityErrorCode errorCode) throws IOException {
        log.warn("JWT rejected [{}] for {} {}", errorCode.getCode(), request.getMethod(), request.getRequestURI());
        SecurityContextHolder.clearContext();
        jwtAuthenticationEntryPoint.commence(request, response, new JwtException(errorCode));
    }
}
