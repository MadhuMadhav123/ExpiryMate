package com.expirymate.auth.controller;

import com.expirymate.auth.dto.ProfileDtos.ChangePasswordRequest;
import com.expirymate.auth.dto.ProfileDtos.UpdateProfileRequest;
import com.expirymate.auth.model.User;
import com.expirymate.auth.repo.UserRepository;
import com.expirymate.auth.security.JwtService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);
    private static final String CLASS_NAME = ProfileController.class.getSimpleName();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public ProfileController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<?> profile(@RequestHeader("Authorization") String authorization) {
        log.info("{} - Get profile API triggered", CLASS_NAME);
        User user = authenticatedUser(authorization);
        if (user == null) {
            log.warn("{} - Invalid token", CLASS_NAME);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid token"));
        }
        log.info("{} - Get profile successful for userId: {}, email: {}", CLASS_NAME, user.getId(), user.getEmail());
        return ResponseEntity.ok(profileResponse(user));
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String authorization,
                                           @Valid @RequestBody UpdateProfileRequest request) {
        log.info("{} - Update profile API triggered for name: {}", CLASS_NAME, request.name());
        User user = authenticatedUser(authorization);
        if (user == null) {
            log.warn("{} - Invalid token for name: {}", CLASS_NAME, request.name());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid token"));
        }

        user.setName(request.name().trim());
        userRepository.save(user);
        log.info("{} - Update profile successful for userId: {}, email: {}", CLASS_NAME, user.getId(), user.getEmail());
        return ResponseEntity.ok(profileResponse(user));
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestHeader("Authorization") String authorization,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("{} - Update password API triggered", CLASS_NAME);
        User user = authenticatedUser(authorization);
        if (user == null) {
            log.warn("{} - Update password Invalid token", CLASS_NAME);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid token"));
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            log.warn("{} - Current password is incorrect for name: {}", CLASS_NAME, user.getName());
            return ResponseEntity.badRequest().body(Map.of("message", "Current password is incorrect"));
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            log.warn("{} - New password must be different for name: {}", CLASS_NAME, user.getName());
            return ResponseEntity.badRequest().body(Map.of("message", "New password must be different"));
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("{} - Password changed successfully for userId: {}, email: {}", CLASS_NAME, user.getId(), user.getEmail());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    private User authenticatedUser(String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return null;
            }
            Claims claims = jwtService.parse(authorization.substring(7));
            Long userId = ((Number) claims.get("userId")).longValue();
            return userRepository.findById(userId).orElse(null);
        } catch (Exception exception) {
            return null;
        }
    }

    private Map<String, Object> profileResponse(User user) {
        String token = jwtService.generate(user.getId(), user.getName(), user.getEmail());
        return Map.of("token", token, "userId", user.getId(), "name", user.getName(), "email", user.getEmail());
    }
}
