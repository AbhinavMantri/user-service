package com.example.user_service.user.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.example.user_service.shared.models.ResponseStatus;
import com.example.user_service.user.dtos.UpdateUserProfileResponse;
import com.example.user_service.user.dtos.UpdateUserRequest;
import com.example.user_service.user.dtos.UserProfileResponse;
import com.example.user_service.user.models.UserProfile;
import com.example.user_service.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    @Test
    void getUserProfileReturnsOkOnSuccess() {
        when(authentication.getName()).thenReturn("user@example.com");
        UserProfile profile = UserProfile.builder()
                .id(UUID.randomUUID().toString())
                .email("user@example.com")
                .fullName("Test User")
                .phone("1234567890")
                .build();

        when(userService.getUserProfile("user@example.com", null, null)).thenReturn(profile);

        ResponseEntity<UserProfileResponse> response = userController.getUserProfile(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ResponseStatus.SUCCESS, response.getBody().getStatus());
        assertEquals("user@example.com", response.getBody().getUserProfile().getEmail());
    }

    @Test
    void getUserProfileReturnsNotFoundOnError() {
        when(authentication.getName()).thenReturn("missing@example.com");
        when(userService.getUserProfile("missing@example.com", null, null)).thenThrow(new RuntimeException("User not found"));

        ResponseEntity<UserProfileResponse> response = userController.getUserProfile(authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ResponseStatus.FAILURE, response.getBody().getStatus());
    }

    @Test
    void updateUserProfileReturnsOkOnSuccess() {
        when(authentication.getName()).thenReturn("user@example.com");
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Updated Name");

        UserProfile updated = UserProfile.builder()
                .id(UUID.randomUUID().toString())
                .email("user@example.com")
                .fullName("Updated Name")
                .phone("1234567890")
                .build();

        when(userService.updateUserProfile("user@example.com", "Updated Name", null)).thenReturn(updated);

        ResponseEntity<UpdateUserProfileResponse> response = userController.updateUserProfile(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ResponseStatus.SUCCESS, response.getBody().getStatus());
        assertEquals("Updated Name", response.getBody().getUserProfile().getFullName());
    }

    @Test
    void updateUserProfileReturnsNotFoundOnError() {
        when(authentication.getName()).thenReturn("missing@example.com");
        UpdateUserRequest request = new UpdateUserRequest();
        request.setPhone("9999999999");

        when(userService.updateUserProfile("missing@example.com", null, "9999999999"))
                .thenThrow(new RuntimeException("User not found"));

        ResponseEntity<UpdateUserProfileResponse> response = userController.updateUserProfile(authentication, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ResponseStatus.FAILURE, response.getBody().getStatus());
    }
}
