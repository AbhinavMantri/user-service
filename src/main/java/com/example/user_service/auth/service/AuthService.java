package com.example.user_service.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.user_service.auth.dtos.LoginRequest;
import com.example.user_service.auth.dtos.RegisterRequest;
import com.example.user_service.auth.exceptions.InvalidCredentialsException;
import com.example.user_service.auth.exceptions.UserAlreadyExistsException;
import com.example.user_service.auth.exceptions.UserDisabledException;
import com.example.user_service.auth.models.RefereshToken;
import com.example.user_service.auth.models.UserSession;
import com.example.user_service.auth.repository.RefereshTokenRepository;
import com.example.user_service.user.models.User;
import com.example.user_service.user.models.UserStatus;
import com.example.user_service.user.repository.UserRepository;

@Service
public class AuthService {
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefereshTokenRepository refreshTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            UserRepository userRepository,
            JWTService jwtService,
            RefreshTokenService refreshTokenService,
            RefereshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public User registerUser(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new UserAlreadyExistsException("User already exists");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole("CUSTOMER");
        user.setStatus(UserStatus.ACTIVE);

        return userRepository.save(user);
    }

    public UserSession login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new UserDisabledException("User account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);
        return new UserSession(accessToken, refreshToken);
    }

    public UserSession refreshSession(String authorizationHeader) {
        String rawRefreshToken = extractToken(authorizationHeader);
        String tokenHash = refreshTokenService.hashToken(rawRefreshToken);

        RefereshToken tokenRecord = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (tokenRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRecord.setRevoked(true);
            refreshTokenRepository.save(tokenRecord);
            throw new InvalidCredentialsException("Refresh token expired");
        }

        User user = tokenRecord.getUser();
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new UserDisabledException("User account is disabled");
        }

        String accessToken = jwtService.generateAccessToken(user);
        return new UserSession(accessToken, rawRefreshToken);
    }

    public void logout(String authorizationHeader) {
        String rawRefreshToken = extractToken(authorizationHeader);
        String tokenHash = refreshTokenService.hashToken(rawRefreshToken);

        RefereshToken tokenRecord = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        tokenRecord.setRevoked(true);
        refreshTokenRepository.save(tokenRecord);
    }

    private String normalizeEmail(String email) {
        return Optional.ofNullable(email)
                .map(String::trim)
                .map(String::toLowerCase)
                .orElse("");
    }

    private String extractToken(String tokenOrHeader) {
        if (tokenOrHeader == null) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }
        String value = tokenOrHeader.trim();
        if (value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            value = value.substring(BEARER_PREFIX.length()).trim();
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        if (value.isEmpty()) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }
        return value;
    }
}
