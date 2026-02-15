package com.example.user_service.user.dtos;

import java.time.LocalDateTime;
import java.util.List;

import com.example.user_service.shared.dtos.ApiResponse;
import com.example.user_service.user.models.UserStatus;

import lombok.Builder;
import lombok.Data;

@Data
public class UserListResponse extends ApiResponse {
    private List<UserSummary> users;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}
