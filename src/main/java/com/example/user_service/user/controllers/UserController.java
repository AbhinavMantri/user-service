package com.example.user_service.user.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_service.shared.models.ResponseStatus;
import com.example.user_service.user.dtos.UpdateUserProfileResponse;
import com.example.user_service.user.dtos.UpdateUserRequest;
import com.example.user_service.user.dtos.UserProfileResponse;
import com.example.user_service.user.models.UserProfile;
import com.example.user_service.user.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@PreAuthorize("isAuthenticated()")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getUserProfile(Authentication authentication) {
        String email = authentication.getName();
        UserProfileResponse response = new UserProfileResponse();
        try {
            UserProfile profile = userService.getUserProfile(email, null, null);
            response.setUserProfile(profile);
            response.setStatus(ResponseStatus.SUCCESS);
            response.setMessage("User profile retrieved successfully");
        } catch (Exception e) {
            response.setStatus(ResponseStatus.FAILURE);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<UpdateUserProfileResponse> updateUserProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequest request) {
        String email = authentication.getName();
        UpdateUserProfileResponse response = new UpdateUserProfileResponse();
        try {
            UserProfile updatedProfile = userService.updateUserProfile(
                    email,
                    request.getName(),
                    request.getPhone());
            response.setUserProfile(updatedProfile);
            response.setStatus(ResponseStatus.SUCCESS);
            response.setMessage("User profile updated successfully");
        } catch (Exception e) {
            response.setStatus(ResponseStatus.FAILURE);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
