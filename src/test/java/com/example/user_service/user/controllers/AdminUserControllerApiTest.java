package com.example.user_service.user.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.user_service.user.models.User;
import com.example.user_service.user.models.UserStatus;
import com.example.user_service.user.service.AdminUserService;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerApiTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController).build();
    }

    @Test
    void getAdminUsersReturnsList() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setRole("USER");
        user.setStatus(UserStatus.ACTIVE);

        Page<User> page = new PageImpl<>(List.of(user));
        when(adminUserService.getAllUsers(eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.users[0].email").value("user@example.com"));
    }

    @Test
    void patchAdminUserStatusReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        User updated = new User();
        updated.setId(id);
        updated.setEmail("user@example.com");
        updated.setRole("USER");
        updated.setStatus(UserStatus.DISABLED);

        when(adminUserService.updateUserStatus(id, UserStatus.DISABLED)).thenReturn(updated);

        mockMvc.perform(patch("/admin/users/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DISABLED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.user.status").value("DISABLED"));
    }

    @Test
    void patchAdminUserRoleReturnsBadRequestWhenMissingUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminUserService.updateUserRole(id, "ADMIN")).thenThrow(new IllegalArgumentException("User not found"));

        mockMvc.perform(patch("/admin/users/{id}/role", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("FAILURE"));
    }
}
