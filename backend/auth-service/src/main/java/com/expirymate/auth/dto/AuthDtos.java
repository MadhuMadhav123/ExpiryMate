package com.expirymate.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

	private AuthDtos() {
	}

	public record RegisterRequest(
			@NotBlank(message = "Full name is required") @Size(min = 2, max = 80, message = "Name must be between 2 and 80 characters") String name,

			@NotBlank(message = "Email is required") @Email(message = "Please enter a valid email address") String email,

			@NotBlank(message = "Password is required") @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters") @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$", message = "Password must contain uppercase, lowercase, number and special character") String password) {
	}

	public record LoginRequest(
			@NotBlank(message = "Email is required") @Email(message = "Please enter a valid email address") String email,

			@NotBlank(message = "Password is required") String password) {
	}

	public record AuthResponse(String token, Long userId, String name, String email) {
	}
}
