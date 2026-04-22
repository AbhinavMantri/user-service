package com.example.user_service.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.user_service.user.models.User;
import com.example.user_service.user.models.UserProfile;
import com.example.user_service.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserProfileReturnsMappedProfile() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setEmail("user@example.com");
        user.setFullName("Test User");
        user.setPhone("1234567890");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        UserProfile profile = userService.getUserProfile("user@example.com", null, null);

        assertEquals(id.toString(), profile.getId());
        assertEquals("user@example.com", profile.getEmail());
        assertEquals("Test User", profile.getFullName());
        assertEquals("1234567890", profile.getPhone());
    }

    @Test
    void getUserProfileThrowsWhenUserMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.getUserProfile("missing@example.com", null, null));
    }

    @Test
    void updateUserProfileUpdatesProvidedFields() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setEmail("user@example.com");
        user.setFullName("Old Name");
        user.setPhone("1111111111");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserProfile updated = userService.updateUserProfile("user@example.com", "New Name", null);

        assertEquals("New Name", updated.getFullName());
        assertEquals("1111111111", updated.getPhone());
        verify(userRepository).save(user);
    }

    @Test
    void updateUserProfileThrowsWhenUserMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.updateUserProfile("missing@example.com", "Name", "123"));
    }
}
