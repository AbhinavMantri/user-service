package com.example.user_service.auth.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.user_service.auth.dtos.LoginRequest;
import com.example.user_service.auth.dtos.RegisterRequest;
import com.example.user_service.auth.exceptions.UserDisabledException;
import com.example.user_service.auth.service.AuthService;
import com.example.user_service.user.models.User;

@ExtendWith(MockitoExtension.class)
class AuthControllerApiTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void registerReturnsCreatedResponse() throws Exception {
        User saved = new User();
        UUID userId = UUID.randomUUID();
        saved.setId(userId);
        saved.setEmail("user@example.com");

        when(authService.registerUser(any(RegisterRequest.class))).thenReturn(saved);

        String body = """
                {
                  "email": "user@example.com",
                  "password": "password123",
                  "fullName": "Test User",
                  "phone": "1234567890"
                }
                """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void registerReturnsBadRequestWhenRequiredEmailOrPasswordMissing() throws Exception {
        String body = """
                {
                  "email": "user@example.com"
                }
                """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void loginReturnsUnauthorizedOnFailure() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new RuntimeException("Invalid credentials"));

        String body = """
                {
                  "email": "user@example.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("FAILURE"));
    }

    @Test
    void refreshReturnsForbiddenForDisabledUser() throws Exception {
        when(authService.refreshSession("Bearer refresh-token"))
                .thenThrow(new UserDisabledException("User account is disabled"));

        mockMvc.perform(post("/auth/refresh")
                .header("Authorization", "Bearer refresh-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value("FAILURE"));
    }

    @Test
    void logoutReturnsOkOnSuccess() throws Exception {
        mockMvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer refresh-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("Logged out successfully"));
    }
}
