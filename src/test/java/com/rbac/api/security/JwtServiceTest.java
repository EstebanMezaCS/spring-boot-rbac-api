package com.rbac.api.security;

import com.rbac.api.entity.Role;
import com.rbac.api.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plain unit tests for JwtService (no Spring context, so they run in milliseconds).
 */
class JwtServiceTest {

    private static final String SECRET = "test-secret-for-unit-tests-only-32-chars-min";

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000);

    private User sampleUser() {
        return new User("alice", "hash", "Alice", "alice@company.com", Role.MANAGER);
    }

    @Test
    void generatedTokenContainsTheUsername() {
        String token = jwtService.generateToken(sampleUser());

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(sampleUser());
        // Change one character inside the signature (last segment) to simulate tampering.
        // The second-to-last char is used because the very last one may only carry padding bits.
        int i = token.length() - 2;
        char replacement = token.charAt(i) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, i) + replacement + token.substring(i + 1);

        assertThatThrownBy(() -> jwtService.extractUsername(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService otherService = new JwtService("a-completely-different-secret-32-chars-long", 3_600_000);
        String foreignToken = otherService.generateToken(sampleUser());

        assertThatThrownBy(() -> jwtService.extractUsername(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        // Negative lifetime: the token is already expired when issued
        JwtService shortLived = new JwtService(SECRET, -1_000);
        String expired = shortLived.generateToken(sampleUser());

        assertThatThrownBy(() -> jwtService.extractUsername(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shortSecretFailsAtStartup() {
        assertThatThrownBy(() -> new JwtService("too-short", 3_600_000))
                .isInstanceOf(IllegalStateException.class);
    }
}
