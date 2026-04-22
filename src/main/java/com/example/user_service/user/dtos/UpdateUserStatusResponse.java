package com.example.user_service.user.dtos;

import com.example.user_service.shared.dtos.ApiResponse;

import lombok.Data;

@Data
public class UpdateUserStatusResponse extends ApiResponse {
    private UserSummary user;
}
