package com.example.user_service.auth.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JWTService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefereshTokenRepository refreshTokenRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtService, refreshTokenService, refreshTokenRepository);
    }

    @Test
    void registerUserThrowsWhenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
            .email("User@Example.com")
            .password("password123")
            .fullName("Test User")
            .phone("1234567890")
            .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(UserAlreadyExistsException.class, () -> authService.registerUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUserSavesNormalizedEmailAndEncodedPassword() {
        RegisterRequest request = RegisterRequest.builder()
            .email(" User@Example.com ")
            .password("password123")
            .fullName("Test User")
            .phone("1234567890")
            .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = authService.registerUser(request);

        assertEquals("user@example.com", saved.getEmail());
        assertNotEquals("password123", saved.getPasswordHash());
        assertTrue(new BCryptPasswordEncoder().matches("password123", saved.getPasswordHash()));
        assertEquals(UserStatus.ACTIVE, saved.getStatus());
        assertEquals("USER", saved.getRole());
    }

    @Test
    void loginReturnsSessionForValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail(" USER@EXAMPLE.COM ");
        request.setPassword("password123");

        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash(new BCryptPasswordEncoder().encode("password123"));
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        UserSession session = authService.login(request);

        assertEquals("access-token", session.getSessionToken());
        assertEquals("refresh-token", session.getRefreshToken());
    }

    @Test
    void loginThrowsWhenUserDisabled() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setStatus(UserStatus.DISABLED);
        user.setPasswordHash(new BCryptPasswordEncoder().encode("password123"));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(UserDisabledException.class, () -> authService.login(request));
    }

    @Test
    void loginThrowsWhenPasswordInvalid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrong-password");

        User user = new User();
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(new BCryptPasswordEncoder().encode("password123"));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void refreshSessionReturnsAccessTokenForValidRefreshToken() {
        String rawRefreshToken = "refresh-token";
        String hashedToken = "hashed-token";

        User user = new User();
        user.setStatus(UserStatus.ACTIVE);

        RefereshToken tokenRecord = new RefereshToken();
        tokenRecord.setUser(user);
        tokenRecord.setRevoked(false);
        tokenRecord.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(refreshTokenService.hashToken(rawRefreshToken)).thenReturn(hashedToken);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(hashedToken)).thenReturn(Optional.of(tokenRecord));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");

        UserSession session = authService.refreshSession("Bearer " + rawRefreshToken);

        assertEquals("new-access-token", session.getSessionToken());
        assertEquals(rawRefreshToken, session.getRefreshToken());
    }

    @Test
    void refreshSessionThrowsWhenTokenExpired() {
        String rawRefreshToken = "refresh-token";
        String hashedToken = "hashed-token";

        User user = new User();
        user.setStatus(UserStatus.ACTIVE);

        RefereshToken tokenRecord = new RefereshToken();
        tokenRecord.setUser(user);
        tokenRecord.setRevoked(false);
        tokenRecord.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(refreshTokenService.hashToken(rawRefreshToken)).thenReturn(hashedToken);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(hashedToken)).thenReturn(Optional.of(tokenRecord));

        assertThrows(InvalidCredentialsException.class, () -> authService.refreshSession("Bearer " + rawRefreshToken));
    }

    @Test
    void refreshSessionThrowsWhenUserDisabled() {
        String rawRefreshToken = "refresh-token";
        String hashedToken = "hashed-token";

        User user = new User();
        user.setStatus(UserStatus.DISABLED);

        RefereshToken tokenRecord = new RefereshToken();
        tokenRecord.setUser(user);
        tokenRecord.setRevoked(false);
        tokenRecord.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(refreshTokenService.hashToken(rawRefreshToken)).thenReturn(hashedToken);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(hashedToken)).thenReturn(Optional.of(tokenRecord));

        assertThrows(UserDisabledException.class, () -> authService.refreshSession(rawRefreshToken));
    }

    @Test
    void logoutRevokesToken() {
        String rawRefreshToken = "refresh-token";
        String hashedToken = "hashed-token";

        RefereshToken tokenRecord = new RefereshToken();
        tokenRecord.setRevoked(false);

        when(refreshTokenService.hashToken(rawRefreshToken)).thenReturn(hashedToken);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(hashedToken)).thenReturn(Optional.of(tokenRecord));

        authService.logout("Bearer " + rawRefreshToken);

        assertTrue(tokenRecord.getRevoked());
        verify(refreshTokenRepository).save(tokenRecord);
    }

    @Test
    void logoutThrowsWhenTokenNotFound() {
        String rawRefreshToken = "refresh-token";
        String hashedToken = "hashed-token";

        when(refreshTokenService.hashToken(rawRefreshToken)).thenReturn(hashedToken);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(hashedToken)).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.logout(rawRefreshToken));
    }
}

