package com.example.user_service.user.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_service.user.dtos.UpdateUserProfileResponse;
import com.example.user_service.user.dtos.UpdateUserRequest;
import com.example.user_service.user.dtos.UserProfileResponse;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getUserProfile() {
        // TODO: Retrieve current authenticated user profile
        UserProfileResponse response = new UserProfileResponse();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<UpdateUserProfileResponse> updateUserProfile(@RequestBody UpdateUserRequest request) {
        // TODO: Update current user profile (optional: name, phone)
        UpdateUserProfileResponse response = new UpdateUserProfileResponse();
        return ResponseEntity.ok(response);
    }
}
