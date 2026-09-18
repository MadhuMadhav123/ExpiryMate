package com.expirymate.auth.controller;

import com.expirymate.auth.dto.ProfileDtos.ChangePasswordRequest;
import com.expirymate.auth.dto.ProfileDtos.UpdateProfileRequest;
import com.expirymate.auth.model.User;
import com.expirymate.auth.repo.UserRepository;
import com.expirymate.auth.security.JwtService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

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
		User user = authenticatedUser(authorization);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid token"));
		}
		return ResponseEntity.ok(profileResponse(user));
	}

	@PutMapping
	public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String authorization,
			@Valid @RequestBody UpdateProfileRequest request) {
		User user = authenticatedUser(authorization);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid token"));
		}

		user.setName(request.name().trim());
		userRepository.save(user);
		return ResponseEntity.ok(profileResponse(user));
	}

	@PutMapping("/password")
	public ResponseEntity<?> changePassword(@RequestHeader("Authorization") String authorization,
			@Valid @RequestBody ChangePasswordRequest request) {
		User user = authenticatedUser(authorization);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid token"));
		}

		if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			return ResponseEntity.badRequest().body(Map.of("message", "Current password is incorrect"));
		}

		if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
			return ResponseEntity.badRequest().body(Map.of("message", "New password must be different"));
		}

		user.setPassword(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
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
