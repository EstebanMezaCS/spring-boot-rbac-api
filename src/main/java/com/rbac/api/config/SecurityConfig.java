package com.rbac.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbac.api.dto.Dtos.ErrorResponse;
import com.rbac.api.repository.UserRepository;
import com.rbac.api.security.JwtAuthenticationFilter;
import com.rbac.api.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Central security rules:
 *  - Stateless: no server session; every request must carry its own JWT.
 *  - Only login, register, and health checks are public; everything else needs a valid token.
 *  - Errors return the same JSON shape as the rest of the API (ErrorResponse).
 */
@Configuration
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtService jwtService, UserRepository userRepository, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF protects cookie-based sessions; a stateless API using Bearer tokens doesn't need it.
            .csrf(AbstractHttpConfigurer::disable)
            // Reuses the "corsFilter" bean from CorsConfig, so browser preflight requests aren't blocked.
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/status", "/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                // No token or invalid token -> 401
                .authenticationEntryPoint((req, res, e) ->
                    writeError(res, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                               "Missing or invalid authentication token"))
                // Valid token but not enough permissions -> 403
                .accessDeniedHandler((req, res, e) ->
                    writeError(res, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                               "You do not have permission to perform this action"))
            )
            // Our JWT filter runs before Spring's default username/password filter.
            .addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository),
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Writes an ErrorResponse as JSON (filters run outside controllers, so @RestControllerAdvice can't help here). */
    private void writeError(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(status, error, message));
    }
}
