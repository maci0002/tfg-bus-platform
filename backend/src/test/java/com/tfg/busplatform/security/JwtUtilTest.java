package com.tfg.busplatform.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de la generación y validación de tokens JWT (firma HS256).
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private UserDetails user(String username) {
        return User.withUsername(username).password("irrelevant").authorities("USER").build();
    }

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Clave de 64 caracteres (>= 256 bits, requisito de HS256).
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "8Zz5tw0Ionm3XPZZfN0NOml3z9FMfmpgXwovR9fp6ryDIoGRM8EPHAB6iHsc0fb");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3_600_000L);
    }

    @Test
    @DisplayName("El token generado codifica el nombre de usuario y es válido")
    void generatesAndValidatesToken() {
        UserDetails alice = user("alice@example.com");
        String token = jwtUtil.generateToken(alice);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice@example.com");
        assertThat(jwtUtil.isTokenValid(token, alice)).isTrue();
    }

    @Test
    @DisplayName("El token de un usuario no es válido para otro usuario distinto")
    void tokenIsNotValidForAnotherUser() {
        String token = jwtUtil.generateToken(user("alice@example.com"));
        assertThat(jwtUtil.isTokenValid(token, user("bob@example.com"))).isFalse();
    }

    @Test
    @DisplayName("Un token expirado es rechazado al validarlo")
    void expiredTokenIsRejected() {
        // Expiración negativa: el token nace ya caducado. jjwt rechaza el token
        // expirado lanzando ExpiredJwtException al parsearlo.
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1_000L);
        UserDetails alice = user("alice@example.com");
        String token = jwtUtil.generateToken(alice);

        assertThatThrownBy(() -> jwtUtil.isTokenValid(token, alice))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
