package com.expirymate.auth.controller;

import com.expirymate.auth.dto.AuthDtos.AuthResponse;
import com.expirymate.auth.dto.AuthDtos.LoginRequest;
import com.expirymate.auth.dto.AuthDtos.RegisterRequest;
import com.expirymate.auth.event.UserRegisteredEvent;
import com.expirymate.auth.messaging.UserEventPublisher;
import com.expirymate.auth.model.User;
import com.expirymate.auth.repo.UserRepository;
import com.expirymate.auth.security.JwtService;
import com.expirymate.auth.service.NotificationClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);
	private static final String CLASS_NAME = AuthController.class.getSimpleName();

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final NotificationClient notificationClient;

	public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			NotificationClient notificationClient) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.notificationClient = notificationClient;
	}

	@Operation(summary = "Register user", description = "Creates a new ExpiryMate user account")
	@ApiResponses({ @ApiResponse(responseCode = "201", description = "Registration successful"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "409", description = "Email is already registered") })
	@PostMapping("/register")
	public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
		String name = request.name().trim();
		String email = normalizeEmail(request.email());

		log.info("{} - Register API triggered for email: {}", CLASS_NAME,email);

		if (userRepository.existsByEmailIgnoreCase(email)) {
			log.warn("{} - Register failed for Email is already registered: {}",CLASS_NAME,email);
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email is already registered"));
		}

		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(request.password()));
		user = userRepository.save(user);

		log.info("{} - Register successful for userId: {}, email: {}",CLASS_NAME,user.getId(),user.getEmail());

		notificationClient.sendWelcomeEmail(user);

		return ResponseEntity.status(HttpStatus.CREATED).body(toAuthResponse(user));
	}

	@Operation(summary = "Login", description = "Authenticates a user and returns a JWT")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Login successful"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Invalid email or password") })
	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

		String email = normalizeEmail(request.email());

		log.info("{} - Login API triggered for email: {}", CLASS_NAME,email);

		return userRepository.findByEmailIgnoreCase(email)
				.filter(user -> passwordEncoder.matches(
						request.password(),
						user.getPassword()
				))
				.<ResponseEntity<?>>map(user -> {
					log.info("{} - Login successful for userId: {}, email: {}",CLASS_NAME,user.getId(),user.getEmail());
					return ResponseEntity.ok(toAuthResponse(user));
				})
				.orElseGet(() -> {
					log.warn("{} - Login failed for email: {}",CLASS_NAME,email);
					return ResponseEntity
							.status(HttpStatus.UNAUTHORIZED)
							.body(Map.of("message","Invalid email or password"));
				});
	}

	private AuthResponse toAuthResponse(User user) {
		String token = jwtService.generate(user.getId(), user.getName(), user.getEmail());
		return new AuthResponse(token, user.getId(), user.getName(), user.getEmail());
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
