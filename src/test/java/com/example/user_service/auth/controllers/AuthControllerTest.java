package com.example.user_service.auth.controllers;

import com.example.user_service.auth.dtos.LoginRequest;
import com.example.user_service.auth.dtos.LoginResponse;
import com.example.user_service.auth.dtos.RegisterRequest;
import com.example.user_service.auth.dtos.RegisterResponse;
import com.example.user_service.auth.exceptions.UserDisabledException;
import com.example.user_service.auth.models.UserSession;
import com.example.user_service.auth.service.AuthService;
import com.example.user_service.shared.models.ResponseStatus;
import com.example.user_service.user.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void registerReturnsCreatedWithSuccessResponse() {
        RegisterRequest request = RegisterRequest.builder()
            .email("user@example.com")
            .password("password123")
            .fullName("Test User")
            .phone("1234567890")
            .build();

        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setId(userId);

        when(authService.registerUser(request)).thenReturn(user);

        ResponseEntity<RegisterResponse> response = authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(ResponseStatus.SUCCESS, response.getBody().getStatus());
        assertEquals("User registered successfully", response.getBody().getMessage());
        assertEquals(userId, response.getBody().getUserId());
    }

    @Test
    void registerReturnsInternalServerErrorWhenUnexpectedExceptionOccurs() {
        RegisterRequest request = RegisterRequest.builder().build();
        when(authService.registerUser(request)).thenThrow(new RuntimeException("Registration failed"));

        ResponseEntity<RegisterResponse> response = authController.register(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ResponseStatus.FAILURE, response.getBody().getStatus());
        assertEquals("Registration failed", response.getBody().getMessage());
    }

    @Test
    void loginReturnsOkOnSuccess() {
        LoginRequest request = new LoginRequest();
        UserSession session = new UserSession("access-token", "refresh-token");
        when(authService.login(request)).thenReturn(session);

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ResponseStatus.SUCCESS, response.getBody().getStatus());
        assertEquals("Login successful", response.getBody().getMessage());
        assertEquals(session, response.getBody().getSession());
    }

    @Test
    void loginReturnsForbiddenForDisabledUser() {
        LoginRequest request = new LoginRequest();
        when(authService.login(request)).thenThrow(new UserDisabledException("User account is disabled"));

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ResponseStatus.FAILURE, response.getBody().getStatus());
    }

    @Test
    void loginReturnsUnauthorizedForOtherErrors() {
        LoginRequest request = new LoginRequest();
        when(authService.login(request)).thenThrow(new RuntimeException("Invalid credentials"));

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(ResponseStatus.FAILURE, response.getBody().getStatus());
    }

    @Test
    void refreshReturnsOkOnSuccess() {
        String token = "Bearer refresh-token";
        UserSession session = new UserSession("access-token", "refresh-token");
        when(authService.refreshSession(token)).thenReturn(session);

        ResponseEntity<?> response = authController.refresh(token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(LoginResponse.class, response.getBody());
        LoginResponse body = (LoginResponse) response.getBody();
        assertEquals(ResponseStatus.SUCCESS, body.getStatus());
        assertEquals("Token refreshed", body.getMessage());
        assertEquals(session, body.getSession());
    }

    @Test
    void refreshReturnsForbiddenForDisabledUser() {
        String token = "Bearer refresh-token";
        when(authService.refreshSession(token)).thenThrow(new UserDisabledException("User account is disabled"));

        ResponseEntity<?> response = authController.refresh(token);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertInstanceOf(LoginResponse.class, response.getBody());
        assertEquals(ResponseStatus.FAILURE, ((LoginResponse) response.getBody()).getStatus());
    }

    @Test
    void refreshReturnsUnauthorizedForOtherErrors() {
        String token = "Bearer refresh-token";
        when(authService.refreshSession(token)).thenThrow(new RuntimeException("Invalid refresh token"));

        ResponseEntity<?> response = authController.refresh(token);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertInstanceOf(LoginResponse.class, response.getBody());
        assertEquals(ResponseStatus.FAILURE, ((LoginResponse) response.getBody()).getStatus());
    }

    @Test
    void logoutReturnsOkOnSuccess() {
        String token = "Bearer refresh-token";
        doNothing().when(authService).logout(token);

        ResponseEntity<?> response = authController.logout(token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Logged out successfully", response.getBody());
    }

    @Test
    void logoutReturnsUnauthorizedOnFailure() {
        String token = "Bearer refresh-token";
        doThrow(new RuntimeException("Invalid refresh token")).when(authService).logout(token);

        ResponseEntity<?> response = authController.logout(token);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid refresh token", response.getBody());
    }
}
