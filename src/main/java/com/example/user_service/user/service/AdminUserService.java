package com.example.user_service.user.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.user_service.user.repository.UserRepository;
import com.example.user_service.user.models.User;
import com.example.user_service.user.models.UserStatus;
import java.util.UUID;

@Service
public class AdminUserService {
    private final UserRepository userRepository;

    @Autowired
    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<User> getAllUsers(String role, UserStatus status, String q, Pageable pageable) {
        List<Specification<User>> specs = new ArrayList<>();
        if (status != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (role != null && !role.isBlank()) {
            specs.add((root, query, cb) -> cb.equal(cb.lower(root.get("role")), role.toLowerCase()));
        }
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.toLowerCase() + "%";
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern)));
        }

        Specification<User> spec = specs.stream().reduce(Specification::and).orElse(null);
        Page<User> usersPage = userRepository.findAll(spec, pageable);
        return usersPage;
    }

    public User updateUserStatus(UUID id, UserStatus status) throws IllegalArgumentException {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus(status);
        return userRepository.save(user);
    }

    public User updateUserRole(UUID id, String role) throws IllegalArgumentException {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(role == null ? null : role.trim().toUpperCase());
        return userRepository.save(user);
    }
}
