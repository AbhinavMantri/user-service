package com.example.user_service.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.user_service.auth.models.RefereshToken;
import com.example.user_service.user.models.User;

public interface RefereshTokenRepository extends JpaRepository<RefereshToken, UUID> {
    Optional<RefereshToken> findByTokenHashAndRevokedFalse(String tokenHash);
    List<RefereshToken> findAllByUserAndRevokedFalseAndExpiresAtAfter(User user, LocalDateTime now);
}
