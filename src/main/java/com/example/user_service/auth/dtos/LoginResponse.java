package com.example.user_service.auth.dtos;

import com.example.user_service.auth.models.UserSession;
import com.example.user_service.shared.dtos.ApiResponse;

import lombok.Data;

@Data
public class LoginResponse extends ApiResponse {
    private UserSession session;
}
