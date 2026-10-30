package com.example.demo.web.dto.content;

import jakarta.validation.constraints.NotBlank;

public record CreateModuleRequest(
		@NotBlank(message = "Module title is required.") String title,
		String description) {
}
