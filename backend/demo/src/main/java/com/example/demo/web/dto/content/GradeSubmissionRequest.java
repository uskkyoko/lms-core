package com.example.demo.web.dto.content;

import jakarta.validation.constraints.NotNull;

public record GradeSubmissionRequest(
		@NotNull(message = "Score is required.") Double points,
		String feedback) {
}
