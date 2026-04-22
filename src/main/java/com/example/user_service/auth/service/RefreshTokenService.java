package com.example.user_service.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.user_service.auth.models.RefereshToken;
import com.example.user_service.auth.repository.RefereshTokenRepository;
import com.example.user_service.user.models.User;

@Service
public class RefreshTokenService {
    private final RefereshTokenRepository refreshTokenRepository;
    private final long refreshTtlDays;

    public RefreshTokenService(
        RefereshTokenRepository refreshTokenRepository,
        @Value("${security.jwt.refresh-ttl-days:7}") long refreshTtlDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTtlDays = refreshTtlDays;
    }

    public String createRefreshToken(User user) {
        revokeActiveTokens(user);
        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        RefereshToken token = RefereshToken.builder()
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(LocalDateTime.now().plusDays(refreshTtlDays))
            .revoked(false)
            .build();

        refreshTokenRepository.save(token);
        return rawToken;
    }

    public void revokeActiveTokens(User user) {
        LocalDateTime now = LocalDateTime.now();
        var activeTokens = refreshTokenRepository.findAllByUserAndRevokedFalseAndExpiresAtAfter(user, now);
        if (activeTokens.isEmpty()) {
            return;
        }
        for (RefereshToken token : activeTokens) {
            token.setRevoked(true);
        }
        refreshTokenRepository.saveAll(activeTokens);
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash refresh token", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
