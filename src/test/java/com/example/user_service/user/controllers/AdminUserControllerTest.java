package com.example.user_service.user.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.user_service.shared.models.ResponseStatus;
import com.example.user_service.user.dtos.UpdateUserRoleResponse;
import com.example.user_service.user.dtos.UpdateUserStatusResponse;
import com.example.user_service.user.dtos.UserListResponse;
import com.example.user_service.user.dtos.UserRoleUpdateRequest;
import com.example.user_service.user.dtos.UserStatusUpdateRequest;
import com.example.user_service.user.models.User;
import com.example.user_service.user.models.UserStatus;
import com.example.user_service.user.service.AdminUserService;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void getAllUsersReturnsOkOnSuccess() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@example.com");
        user.setFullName("Admin User");
        user.setRole("ADMIN");
        user.setStatus(UserStatus.ACTIVE);

        Page<User> page = new PageImpl<>(List.of(user));
        when(adminUserService.getAllUsers(eq("ADMIN"), eq(UserStatus.ACTIVE), eq("adm"), any(Pageable.class))).thenReturn(page);

        ResponseEntity<UserListResponse> response = adminUserController.getAllUsers(0, 20, "createdAt", "desc", UserStatus.ACTIVE, "ADMIN", "adm");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ResponseStatus.SUCCESS, response.getBody().getStatus());
        assertEquals(1, response.getBody().getUsers().size());
    }

    @Test
    void getAllUsersReturnsBadRequestOnError() {
        when(adminUserService.getAllUsers(eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenThrow(new RuntimeException("Query failed"));

        ResponseEntity<UserListResponse> response = adminUserController.getAllUsers(0, 20, "createdAt", "desc", null, null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ResponseStatus.FAILURE, response.getBody().getStatus());
    }

    @Test
    void updateUserStatusReturnsOkOnSuccess() {
        UUID userId = UUID.randomUUID();
        User updated = new User();
        updated.setId(userId);
        updated.setEmail("user@example.com");
        updated.setRole("USER");
        updated.setStatus(UserStatus.DISABLED);

        UserStatusUpdateRequest request = new UserStatusUpdateRequest();
        request.setStatus(UserStatus.DISABLED);

        when(adminUserService.updateUserStatus(userId, UserStatus.DISABLED)).thenReturn(updated);

        ResponseEntity<UpdateUserStatusResponse> response = adminUserController.updateUserStatus(userId.toString(), request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ResponseStatus.SUCCESS, response.getBody().getStatus());
        assertEquals(UserStatus.DISABLED, response.getBody().getUser().getStatus());
    }

    @Test
    void updateUserStatusReturnsBadRequestOnIllegalArgument() {
        UUID userId = UUID.randomUUID();
        UserStatusUpdateRequest request = new UserStatusUpdateRequest();
        request.setStatus(UserStatus.ACTIVE);

        when(adminUserService.updateUserStatus(userId, UserStatus.ACTIVE))
                .thenThrow(new IllegalArgumentException("User not found"));

        ResponseEntity<UpdateUserStatusResponse> response = adminUserController.updateUserStatus(userId.toString(), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ResponseStatus.FAILURE, response.getBody().getStatus());
    }

    @Test
    void updateUserRoleReturnsOkOnSuccess() {
        UUID userId = UUID.randomUUID();
        User updated = new User();
        updated.setId(userId);
        updated.setEmail("user@example.com");
        updated.setRole("ADMIN");
        updated.setStatus(UserStatus.ACTIVE);

        UserRoleUpdateRequest request = new UserRoleUpdateRequest();
        request.setRole("ADMIN");

        when(adminUserService.updateUserRole(userId, "ADMIN")).thenReturn(updated);

        ResponseEntity<UpdateUserRoleResponse> response = adminUserController.updateUserRole(userId.toString(), request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ResponseStatus.SUCCESS, response.getBody().getStatus());
        assertEquals("ADMIN", response.getBody().getUser().getRole());
    }

    @Test
    void updateUserRoleReturnsInternalServerErrorOnUnexpectedError() {
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = new UserRoleUpdateRequest();
        request.setRole("ADMIN");

        when(adminUserService.updateUserRole(userId, "ADMIN")).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<UpdateUserRoleResponse> response = adminUserController.updateUserRole(userId.toString(), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ResponseStatus.FAILURE, response.getBody().getStatus());
    }
}
