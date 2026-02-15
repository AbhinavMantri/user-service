package com.example.user_service.user.dtos;

import com.example.user_service.shared.dtos.ApiResponse;
import com.example.user_service.user.models.UserProfile;

import lombok.Data;

@Data
public class UpdateUserProfileResponse extends ApiResponse {
    private UserProfile userProfile;
}
