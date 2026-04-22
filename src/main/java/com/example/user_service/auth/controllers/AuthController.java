package com.example.user_service.auth.controllers;

import com.example.user_service.auth.dtos.LoginRequest;
import com.example.user_service.auth.dtos.LoginResponse;
import com.example.user_service.auth.dtos.RegisterRequest;
import com.example.user_service.auth.dtos.RegisterResponse;
import com.example.user_service.auth.exceptions.UserAlreadyExistsException;
import com.example.user_service.auth.models.UserSession;
import com.example.user_service.auth.service.AuthService;
import com.example.user_service.auth.exceptions.UserDisabledException;
import com.example.user_service.shared.models.ResponseStatus;
import com.example.user_service.user.models.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String REQUEST_ID = "requestId";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        setRequestIdIfMissing();
        try {
            String logGroup = requestGroup("REGISTER");
            log.info("{} request received for email={}", logGroup, request.getEmail());
            RegisterResponse response = new RegisterResponse();
            try {
                User user = authService.registerUser(request);
                response.setStatus(ResponseStatus.SUCCESS);
                response.setMessage("User registered successfully");
                response.setUserId(user.getId());
                response.setEmail(user.getEmail());
                response.setFullName(user.getFullName());
                response.setPhone(user.getPhone());
                log.info("{} success userId={} email={}", logGroup, user.getId(), user.getEmail());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } catch (UserAlreadyExistsException e) {
                response.setStatus(ResponseStatus.FAILURE);
                response.setMessage(e.getMessage());
                log.warn("{} duplicate email={} reason={}", logGroup, request.getEmail(), e.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            } catch (IllegalArgumentException e) {
                response.setStatus(ResponseStatus.FAILURE);
                response.setMessage(e.getMessage());
                log.warn("{} bad-request email={} reason={}", logGroup, request.getEmail(), e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            } catch (Exception e) {
                response.setStatus(ResponseStatus.FAILURE);
                response.setMessage(e.getMessage());
                log.warn("{} failure email={} reason={}", logGroup, request.getEmail(), e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } finally {
            MDC.remove(REQUEST_ID);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        setRequestIdIfMissing();
        try {
            String logGroup = requestGroup("LOGIN");
            log.info("{} request received for email={}", logGroup, request.getEmail());
            LoginResponse response = new LoginResponse();
            try {
                UserSession session = authService.login(request);
                response.setSession(session);
                response.setStatus(ResponseStatus.SUCCESS);
                response.setMessage("Login successful");
                log.info("{} success email={}", logGroup, request.getEmail());
                return ResponseEntity.ok(response);
            } catch (UserDisabledException e) {
                response.setStatus(ResponseStatus.FAILURE);
                response.setMessage(e.getMessage());
                log.warn("{} blocked email={} reason={}", logGroup, request.getEmail(), e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            } catch (Exception e) {
                response.setStatus(ResponseStatus.FAILURE);
                response.setMessage(e.getMessage());
                log.warn("{} failure email={} reason={}", logGroup, request.getEmail(), e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } finally {
            MDC.remove(REQUEST_ID);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestHeader("Authorization") String refreshToken) {
        setRequestIdIfMissing();
        try {
            String logGroup = requestGroup("REFRESH");
            log.info("{} request received hasBearerPrefix={}", logGroup, refreshToken != null && refreshToken.startsWith("Bearer "));
            LoginResponse response = new LoginResponse();
            try {
                UserSession session = authService.refreshSession(refreshToken);
                response.setSession(session);
                response.setStatus(ResponseStatus.SUCCESS);
                response.setMessage("Token refreshed");
                log.info("{} success", logGroup);
                return ResponseEntity.ok(response);
            } catch (UserDisabledException e) {
                response.setStatus(ResponseStatus.FAILURE);
                response.setMessage(e.getMessage());
                log.warn("{} blocked reason={}", logGroup, e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            } catch (Exception e) {
                response.setStatus(ResponseStatus.FAILURE);
                response.setMessage(e.getMessage());
                log.warn("{} failure reason={}", logGroup, e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } finally {
            MDC.remove(REQUEST_ID);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String accessToken) {
        try {
            setRequestIdIfMissing();
            String logGroup = requestGroup("LOGOUT");
            log.info("{} request received hasBearerPrefix={}", logGroup, accessToken != null && accessToken.startsWith("Bearer "));
            try {
                authService.logout(accessToken);
                log.info("{} success", logGroup);
                return ResponseEntity.ok("Logged out successfully");
            } catch (Exception e) {
                log.warn("{} failure reason={}", logGroup, e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
            }
        } finally {
            MDC.remove(REQUEST_ID);
        }
    }

    private String requestGroup(String endpoint) {
        return "[AUTH_CONTROLLER] [" + endpoint + "] [requestId=" + MDC.get(REQUEST_ID) + "]";
    }

    private void setRequestIdIfMissing() {
        if (MDC.get(REQUEST_ID) == null) {
            MDC.put(REQUEST_ID, UUID.randomUUID().toString());
        }
    }
}
