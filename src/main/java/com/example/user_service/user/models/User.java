package com.example.user_service.models;

import java.time.LocalDateTime;

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
        @Column(name = "email", nullable = false, unique = true, length = 254)
        private String email;

        @Column(name = "password_hash", nullable = false)
        private String passwordHash;

        @Column(name = "full_name")
        private String fullName;

        @Column(name = "phone")
        private String phone;

        @Column(name = "role")
        private String role;

        @Enumerated(EnumType.STRING)
        @Column(name = "status")
        private UserStatus status;

        @CreationTimestamp
        @Column(name = "created_at", updatable = false)
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at")
        private LocalDateTime updatedAt;
}
