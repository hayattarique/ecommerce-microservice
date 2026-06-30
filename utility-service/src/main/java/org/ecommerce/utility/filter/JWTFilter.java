package org.ecommerce.utility.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.utility.constants.SecurityConstants;
import org.ecommerce.utility.model.AuthUserDetails;
import org.ecommerce.utility.service.JWTService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JWTFilter extends OncePerRequestFilter {

    private final JWTService jwtService;


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
        if (!jwtService.validateToken(token)) {
            log.warn("Invalid JWT token: {}", token);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        // check only access token is used in access token context, refresh token should not be used here
        if (jwtService.isRefreshToken(token)) {
            log.warn("Refresh token used in access token context: {}", token);
            filterChain.doFilter(request, response);
            return;
        }
        // Avoid duplicate authentication
        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Long userId = jwtService.extractUserId(token);
                String email = jwtService.extractEmail(token);
                List<String> roles = jwtService.extractRoles(token);
                List<String> permissions = jwtService.extractPermissions(token);

                List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
                roles.forEach(role -> grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                permissions.forEach(permission -> grantedAuthorities.add(new SimpleGrantedAuthority(permission)));
                AuthUserDetails authUserDetails = AuthUserDetails.builder()
                        .userId(userId)
                        .email(email)
                        .enabled(true)
                        .accountNonExpired(true)
                        .accountNonLocked(true)
                        .credentialsNonExpired(true)
                        .roles(roles)
                        .permissions(permissions)
                        .build();
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        authUserDetails, token, grantedAuthorities);
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);


            }
        } catch (Exception e) {
            log.error("Authentication failed.", e);
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);


    }
}
