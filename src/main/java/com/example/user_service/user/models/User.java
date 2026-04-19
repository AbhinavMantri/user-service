package com.example.user_service.user.models;

import java.time.LocalDateTime;

import com.example.user_service.shared.models.BaseModel;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseModel {
        @Column(name = "email", nullable = false, unique = true, length = 320)
        private String email;

        @Column(name = "password_hash", nullable = false)
        private String passwordHash;

        @Column(name = "full_name")
        private String fullName;

        @Column(name = "phone")
        private String phone;

        @Column(name = "role", nullable = false, length = 32)
        private String role;

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false, length = 32)
        private UserStatus status;

        @CreationTimestamp
        @Column(name = "created_at", updatable = false)
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at")
        private LocalDateTime updatedAt;
}
