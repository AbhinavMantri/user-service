package com.example.user_service.user.dtos;

import java.time.LocalDateTime;

import com.example.user_service.user.models.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {
    private String id;
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
