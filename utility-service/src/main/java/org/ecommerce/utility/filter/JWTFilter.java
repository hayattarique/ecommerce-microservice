package org.ecommerce.utility.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.utility.config.PermissionProvider;
import org.ecommerce.utility.constants.SecurityConstants;
import org.ecommerce.utility.model.AuthUserDetails;
import org.ecommerce.utility.service.JWTService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JWTFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final PermissionProvider permissionProvider;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        header = header == null ? "" : header.trim();
        if (!header.startsWith(SecurityConstants.AUTHORIZATION_HEADER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String token = header.substring(SecurityConstants.AUTHORIZATION_HEADER_PREFIX.length());
            String username = jwtService.extractUsername(token);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtService.validateToken(token)) {
                    List<String> roles = jwtService.extractRole(token);
                    List<String> permission = permissionProvider.getPermissionForUser(username);
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    if (roles != null && !roles.isEmpty()) {
                        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                    }
                    if (permission != null && !permission.isEmpty()) {
                        permission.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));
                    }

                    AuthUserDetails authUserDetails = AuthUserDetails.builder().email(username).roles(roles).build();
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(authUserDetails, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }

            }
        } catch (Exception e) {
            log.error("Error processing JWT: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);


    }
}
