package com.example.demo.web.dto.content;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateAssignmentRequest(
		@NotBlank(message = "Assignment title is required.") String title,
		String instructions,
		Long moduleId,
		@NotNull(message = "Deadline is required.") LocalDateTime deadline,
		@Positive(message = "Max points must be greater than zero.") Double maxPoints,
		Boolean published) {
}
