package com.example.user_service.user.dtos;

import com.example.user_service.user.models.UserStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusUpdateRequest {
    @NotNull
    private UserStatus status;
}
