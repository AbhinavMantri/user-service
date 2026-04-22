package com.example.user_service.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.user_service.user.models.User;
import com.example.user_service.user.models.UserProfile;
import com.example.user_service.user.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfile getUserProfile(String email, String name, String phone) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserProfile profile = UserProfile.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .build();       
        return profile;
    }

    public UserProfile updateUserProfile(String email, String name, String phone) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (name != null) {
            user.setFullName(name);
        }
        if (phone != null) {
            user.setPhone(phone);
        }

        User updatedUser = userRepository.save(user);
        return UserProfile.builder()
                .id(updatedUser.getId().toString())
                .email(updatedUser.getEmail())
                .fullName(updatedUser.getFullName())
                .phone(updatedUser.getPhone())
                .build();
    }
}
