package com.example.user_service.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.user_service.user.models.User;

class JWTServiceTest {

    @Test
    void generateAndValidateTokenReturnsClaims() {
        JWTService jwtService = new JWTService("secret-key", "user-service", 900);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setRole("ADMIN");

        String token = jwtService.generateAccessToken(user);
        assertNotNull(token);

        Map<String, Object> claims = jwtService.validateAndExtractClaims(token);

        assertEquals("user@example.com", claims.get("email"));
        assertEquals("ADMIN", claims.get("role"));
        assertEquals("user-service", claims.get("iss"));
    }

    @Test
    void validateAndExtractClaimsThrowsForTamperedToken() {
        JWTService jwtService = new JWTService("secret-key", "user-service", 900);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setRole("USER");

        String token = jwtService.generateAccessToken(user);
        String tampered = token + "x";

        assertThrows(IllegalArgumentException.class, () -> jwtService.validateAndExtractClaims(tampered));
    }
}
