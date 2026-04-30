package com.payments.gateway.controller;

import com.payments.gateway.dto.UserDTO;
import com.payments.gateway.entity.UserEntity;
import com.payments.gateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        // Try to find the seeded user by email first
        return userRepository.findByEmail("roshan@gmail.com")
                .map(user -> ResponseEntity.ok(toDTO(user)))
                .orElseGet(() -> {
                    // Fallback: return the first user in the DB
                    List<UserEntity> allUsers = userRepository.findAll();
                    if (allUsers.isEmpty()) {
                        return ResponseEntity.notFound().build();
                    }
                    return ResponseEntity.ok(toDTO(allUsers.get(0)));
                });
    }

    private UserDTO toDTO(UserEntity user) {
        return UserDTO.builder()
                .id(user.getId().toString())
                .name(user.getFullName() != null ? user.getFullName() : "User")
                .email(user.getEmail())
                .build();
    }
}
