package com.example.user_service.user.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_service.user.dtos.UpdateUserRoleResponse;
import com.example.user_service.user.dtos.UpdateUserStatusResponse;
import com.example.user_service.user.dtos.UserListResponse;
import com.example.user_service.user.dtos.UserRoleUpdateRequest;
import com.example.user_service.user.dtos.UserStatusUpdateRequest;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @GetMapping
    public ResponseEntity<UserListResponse> getAllUsers() {
        // TODO: Retrieve all users with pagination/filtering
        UserListResponse response = new UserListResponse();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UpdateUserStatusResponse> updateUserStatus(@PathVariable String id, @RequestBody UserStatusUpdateRequest request) {
        // TODO: Update user status (ACTIVE, DISABLED)
        UpdateUserStatusResponse response = new UpdateUserStatusResponse();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UpdateUserRoleResponse> updateUserRole(@PathVariable String id, @RequestBody UserRoleUpdateRequest request) {
        // TODO: Update user role
        UpdateUserRoleResponse response = new UpdateUserRoleResponse();
        return ResponseEntity.ok(response);
    }
}
