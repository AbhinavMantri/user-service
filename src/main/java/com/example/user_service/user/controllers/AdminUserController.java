package com.example.user_service.user.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_service.shared.models.ResponseStatus;
import com.example.user_service.user.dtos.UpdateUserRoleResponse;
import com.example.user_service.user.dtos.UpdateUserStatusResponse;
import com.example.user_service.user.dtos.UserListResponse;
import com.example.user_service.user.dtos.UserRoleUpdateRequest;
import com.example.user_service.user.dtos.UserStatusUpdateRequest;
import com.example.user_service.user.dtos.UserSummary;
import com.example.user_service.user.models.User;
import com.example.user_service.user.models.UserStatus;
import com.example.user_service.user.service.AdminUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<UserListResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String q) {
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);

        UserListResponse response = new UserListResponse();
        try {
            Page<User> userPage = adminUserService.getAllUsers(role, status, q, pageable);
            response.setUsers(from(userPage));
            response.setTotalElements(userPage.getTotalElements());
            response.setTotalPages(userPage.getTotalPages());
            response.setPage(userPage.getNumber());
            response.setSize(userPage.getSize());
            response.setStatus(ResponseStatus.SUCCESS);
            response.setMessage("Users retrieved successfully"); 
        } catch (Exception e) {
            response.setStatus(ResponseStatus.FAILURE);
            response.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
       
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UpdateUserStatusResponse> updateUserStatus(@PathVariable String id, @Valid @RequestBody UserStatusUpdateRequest request) {
        UpdateUserStatusResponse response = new UpdateUserStatusResponse();
        try {
            UUID userId = UUID.fromString(id);
            User updatedUser = adminUserService.updateUserStatus(userId, request.getStatus());
            UserSummary summary = UserSummary.builder()
                    .id(updatedUser.getId().toString())
                    .email(updatedUser.getEmail())
                    .fullName(updatedUser.getFullName())
                    .phone(updatedUser.getPhone())
                    .role(updatedUser.getRole())
                    .status(updatedUser.getStatus())
                    .createdAt(updatedUser.getCreatedAt())
                    .updatedAt(updatedUser.getUpdatedAt())
                    .build();

            response.setUser(summary);
            response.setStatus(ResponseStatus.SUCCESS);
            response.setMessage("User status updated successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.setStatus(ResponseStatus.FAILURE);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setStatus(ResponseStatus.FAILURE);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UpdateUserRoleResponse> updateUserRole(@PathVariable String id, @Valid @RequestBody UserRoleUpdateRequest request) {
        UpdateUserRoleResponse response = new UpdateUserRoleResponse();
        try {
            UUID userId = UUID.fromString(id);
            User updatedUser = adminUserService.updateUserRole(userId, request.getRole());
            UserSummary summary = UserSummary.builder()
                    .id(updatedUser.getId().toString())
                    .email(updatedUser.getEmail())
                    .fullName(updatedUser.getFullName())
                    .phone(updatedUser.getPhone())
                    .role(updatedUser.getRole())
                    .status(updatedUser.getStatus())
                    .createdAt(updatedUser.getCreatedAt())
                    .updatedAt(updatedUser.getUpdatedAt())
                    .build();

            response.setUser(summary);
            response.setStatus(ResponseStatus.SUCCESS);
            response.setMessage("User role updated successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.setStatus(ResponseStatus.FAILURE);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setStatus(ResponseStatus.FAILURE);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private List<UserSummary> from(Page<User> userPage) {
        List<UserSummary> summaries = new ArrayList<>();
        for (User user : userPage.getContent()) {
            UserSummary summary = UserSummary.builder()
                    .id(user.getId().toString())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .phone(user.getPhone())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();
           
            summaries.add(summary);
        }
        return summaries;
    }
}
