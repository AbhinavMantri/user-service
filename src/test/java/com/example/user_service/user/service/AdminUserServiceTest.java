package com.example.user_service.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.user_service.user.models.User;
import com.example.user_service.user.models.UserStatus;
import com.example.user_service.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void getAllUsersDelegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        User user = new User();
        user.setId(UUID.randomUUID());
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<User> result = adminUserService.getAllUsers("ADMIN", UserStatus.ACTIVE, "admin", pageable);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void updateUserStatusUpdatesAndSavesUser() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updated = adminUserService.updateUserStatus(id, UserStatus.DISABLED);

        assertEquals(UserStatus.DISABLED, updated.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void updateUserStatusThrowsWhenUserMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> adminUserService.updateUserStatus(id, UserStatus.ACTIVE));
    }

    @Test
    void updateUserRoleUpdatesAndSavesUser() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setRole("USER");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updated = adminUserService.updateUserRole(id, "ADMIN");

        assertEquals("ADMIN", updated.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void updateUserRoleThrowsWhenUserMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> adminUserService.updateUserRole(id, "ADMIN"));
    }
}
