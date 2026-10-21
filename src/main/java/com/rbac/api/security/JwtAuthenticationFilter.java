package com.rbac.api.security;

import com.rbac.api.entity.User;
import com.rbac.api.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs once per request. If the request carries "Authorization: Bearer <token>",
 * it verifies the token, loads the user, and registers them as authenticated.
 * If there is no token or it is invalid, the request continues unauthenticated
 * and Spring Security rejects it later with 401 when the endpoint is protected.
 *
 * Not annotated with @Component on purpose: SecurityConfig creates it, so it is
 * registered only inside the security filter chain (not twice as a servlet filter).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                String username = jwtService.extractUsername(token);

                // Load the user from the DB so role changes and deactivation apply immediately,
                // even if the token was issued before the change.
                userRepository.findByUsername(username)
                        .filter(User::isActive)
                        .ifPresent(user -> authenticate(user, request));
            } catch (JwtException | IllegalArgumentException ex) {
                // Invalid, tampered, or expired token: leave the request unauthenticated.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Converts the user's role and permissions into Spring Security authorities. */
    private void authenticate(User user, HttpServletRequest request) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())); // e.g. ROLE_ADMINISTRATOR
        user.getRole().getPermissions()
                .forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));            // e.g. users.delete

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
