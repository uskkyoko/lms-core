package com.example.demo.web.dto.content;

import com.example.demo.domain.MaterialType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMaterialRequest(
		@NotBlank(message = "Material title is required.") String title,
		@NotNull(message = "Material type is required.") MaterialType type,
		String body,
		String embedUrl,
		Boolean published) {
}
