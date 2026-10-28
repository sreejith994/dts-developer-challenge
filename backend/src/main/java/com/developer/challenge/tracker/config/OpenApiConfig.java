package com.developer.challenge.tracker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI trackerOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Caseworker Task Tracker API")
				.description("API for HMCTS caseworkers to create and manage their tasks")
				.version("v1"));
	}

}
