package com.expirymate.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ProfileDtos {

	private ProfileDtos() {
	}

	public record UpdateProfileRequest(
			@NotBlank(message = "Name is required") @Size(min = 2, max = 80, message = "Name must be between 2 and 80 characters") String name) {
	}

	public record ChangePasswordRequest(@NotBlank(message = "Current password is required") String currentPassword,

			@NotBlank(message = "New password is required") @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters") @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$", message = "Password must contain uppercase, lowercase, number and special character") String newPassword) {
	}
}
