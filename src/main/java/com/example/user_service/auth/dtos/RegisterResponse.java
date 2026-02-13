package com.example.user_service.auth.dtos;

import java.util.UUID;

import com.example.user_service.shared.dtos.ApiResponse;

import lombok.Data;

@Data
public class RegisterResponse extends ApiResponse {
    private UUID userId;
    private String email;
    private String fullName;
    private String phone;
}
