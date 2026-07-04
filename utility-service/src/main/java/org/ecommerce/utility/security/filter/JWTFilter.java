package org.ecommerce.utility.security.filter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.utility.commons.constants.SecurityConstants;
import org.ecommerce.utility.security.exception.JwtAuthenticationException;
import org.ecommerce.utility.security.model.AuthenticatedUser;
import org.ecommerce.utility.security.service.JwtClaimExtractorService;
import org.ecommerce.utility.security.service.JwtTokenValidatorService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JWTFilter extends OncePerRequestFilter {

    private final JwtTokenValidatorService jwtTokenValidator;
    private final JwtClaimExtractorService jwtClaimExtractor;


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
        // step 2 validate token
        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Claims claims = jwtTokenValidator.validateTokenAndGetClaims(token);
                AuthenticatedUser userDetails = jwtClaimExtractor.extractUserDetailsFromToken(claims);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }


        } catch (JwtAuthenticationException e) {
            log.error("Error occurred while processing JWT token", e);
            SecurityContextHolder.clearContext();
            throw new BadCredentialsException("Invalid JWT token: " + e.getMessage(), e);
        }
        filterChain.doFilter(request, response);

    }
}
