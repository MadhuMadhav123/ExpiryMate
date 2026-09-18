package com.expirymate.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI notificationServiceOpenApi() {
		return new OpenAPI()
				.info(new Info().title("ExpiryMate Notification Service").version("1.0")
						.description("Registration emails, expiry reminders and notification logs"))
				.servers(List.of(new Server().url("/")));
	}
}
