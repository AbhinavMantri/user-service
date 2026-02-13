package com.example.user_service.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.example.user_service.auth.exceptions.InvalidCredentialsException;
import com.example.user_service.auth.exceptions.UserAlreadyExistsException;
import com.example.user_service.auth.exceptions.UserDisabledException;
import com.example.user_service.auth.models.RefereshToken;
import com.example.user_service.auth.repository.RefereshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.user_service.auth.dtos.LoginRequest;
import com.example.user_service.auth.dtos.RegisterRequest;
import com.example.user_service.auth.models.UserSession;
import com.example.user_service.user.models.User;
import com.example.user_service.user.models.UserStatus;
import com.example.user_service.user.repository.UserRepository;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String REQUEST_ID = "requestId";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JWTService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefereshTokenRepository refreshTokenRepository;

    @Autowired
    public AuthService(
        UserRepository userRepository,
        JWTService jwtService,
        RefreshTokenService refreshTokenService,
        RefereshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public User registerUser(RegisterRequest request) throws UserAlreadyExistsException {
        String logGroup = operationGroup("REGISTER_USER");
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        log.info("{} request received email={}", logGroup, normalizedEmail);

        // Check if email already exists
        Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);
        
        if (existingUser.isPresent()) {
            log.warn("{} duplicate email={}", logGroup, normalizedEmail);
            throw new UserAlreadyExistsException("Email is already registered");
        }

        // Create new user
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole("USER");
        user.setStatus(UserStatus.ACTIVE);

        // Save user to database
        User savedUser = userRepository.save(user);
        log.info("{} success userId={} email={}", logGroup, savedUser.getId(), savedUser.getEmail());
        return savedUser;
    }

    public UserSession login(LoginRequest request) throws InvalidCredentialsException, UserDisabledException {
        String logGroup = operationGroup("LOGIN");
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        log.info("{} request received email={}", logGroup, normalizedEmail);

        User user = userRepository.findByEmail(normalizedEmail).orElseThrow(
            () -> {
                log.warn("{} invalid credentials email={} reason=user_not_found", logGroup, normalizedEmail);
                return new InvalidCredentialsException("Invalid email or password");
            }
        );
    
        if (user.getStatus() == UserStatus.DISABLED) {
            log.warn("{} blocked email={} reason=user_disabled", logGroup, normalizedEmail);
            throw new UserDisabledException("User account is disabled.Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("{} invalid credentials email={} reason=password_mismatch", logGroup, normalizedEmail);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);
        log.info("{} success userId={} email={}", logGroup, user.getId(), normalizedEmail);

        return new UserSession(accessToken, refreshToken);
    }

    public UserSession refreshSession(String authorizationHeader) throws InvalidCredentialsException, UserDisabledException {
        String logGroup = operationGroup("REFRESH_SESSION");
        log.info("{} request received hasBearerPrefix={}", logGroup, authorizationHeader != null && authorizationHeader.startsWith("Bearer "));

        String rawRefreshToken = extractTokenFromAuthorizationHeader(authorizationHeader);
        String tokenHash = refreshTokenService.hashToken(rawRefreshToken);

        RefereshToken tokenRecord = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash).orElseThrow(
            () -> {
                log.warn("{} invalid token reason=not_found_or_revoked", logGroup);
                return new InvalidCredentialsException("Invalid refresh token");
            }
        );

        if (tokenRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("{} invalid token reason=expired", logGroup);
            throw new InvalidCredentialsException("Refresh token has expired");
        }

        User user = tokenRecord.getUser();
        if (user.getStatus() == UserStatus.DISABLED) {
            log.warn("{} blocked userId={} reason=user_disabled", logGroup, user.getId());
            throw new UserDisabledException("User account is disabled.Please contact support.");
        }

        String accessToken = jwtService.generateAccessToken(user);
        log.info("{} success userId={}", logGroup, user.getId());
        return new UserSession(accessToken, rawRefreshToken);
    }

    public void logout(String authorizationHeader) {
        String logGroup = operationGroup("LOGOUT");
        log.info("{} request received hasBearerPrefix={}", logGroup, authorizationHeader != null && authorizationHeader.startsWith("Bearer "));

        String rawRefreshToken = extractTokenFromAuthorizationHeader(authorizationHeader);
        String tokenHash = refreshTokenService.hashToken(rawRefreshToken);

        RefereshToken tokenRecord = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash).orElseThrow(
            () -> {
                log.warn("{} invalid token reason=not_found_or_revoked", logGroup);
                return new InvalidCredentialsException("Invalid refresh token");
            }
        );

        tokenRecord.setRevoked(true);
        refreshTokenRepository.save(tokenRecord);
        log.info("{} success userId={}", logGroup, tokenRecord.getUser() != null ? tokenRecord.getUser().getId() : null);
    }

    private String extractTokenFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new InvalidCredentialsException("Refresh token is required");
        }

        if (authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = authorizationHeader.substring(7).trim();
            if (token.isEmpty()) {
                throw new InvalidCredentialsException("Refresh token is required");
            }
            return token;
        }

        return authorizationHeader.trim();
    }

    private String operationGroup(String operation) {
        String requestId = MDC.get(REQUEST_ID);
        if (requestId == null) {
            requestId = "N/A";
        }
        return "[AUTH_SERVICE] [" + operation + "] [requestId=" + requestId + "]";
    }
}
