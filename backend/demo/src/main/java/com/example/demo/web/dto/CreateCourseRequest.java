package com.example.demo.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCourseRequest(
		@NotBlank(message = "Title is required.") String title,
		@NotBlank(message = "Course code is required.") String code,
		String description,
		@NotBlank(message = "Term is required.") String term) {
}
