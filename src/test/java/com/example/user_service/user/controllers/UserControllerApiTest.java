package com.example.user_service.user.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.user_service.user.models.UserProfile;
import com.example.user_service.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerApiTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    void getMeReturnsProfile() throws Exception {
        UserProfile profile = UserProfile.builder()
                .id("u1")
                .email("user@example.com")
                .fullName("Test User")
                .phone("1234567890")
                .build();

        when(userService.getUserProfile("user@example.com", null, null)).thenReturn(profile);

        mockMvc.perform(get("/users/me").principal(new UsernamePasswordAuthenticationToken("user@example.com", "n/a")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.userProfile.email").value("user@example.com"));
    }

    @Test
    void patchMeReturnsUpdatedProfile() throws Exception {
        UserProfile updated = UserProfile.builder()
                .id("u1")
                .email("user@example.com")
                .fullName("Updated Name")
                .phone("1234567890")
                .build();

        when(userService.updateUserProfile("user@example.com", "Updated Name", null)).thenReturn(updated);

        mockMvc.perform(patch("/users/me")
                .principal(new UsernamePasswordAuthenticationToken("user@example.com", "n/a"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated Name\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.userProfile.fullName").value("Updated Name"));
    }

    @Test
    void patchMeReturnsNotFoundWhenServiceFails() throws Exception {
        when(userService.updateUserProfile("user@example.com", null, "9999999999"))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(patch("/users/me")
                .principal(new UsernamePasswordAuthenticationToken("user@example.com", "n/a"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"9999999999\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("FAILURE"));
    }
}
