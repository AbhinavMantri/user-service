package com.example.user_service.shared.dtos;

import com.example.user_service.shared.models.ResponseStatus;

import lombok.Data;

@Data
public class ApiResponse {
    private ResponseStatus status;
    private String message;
}
