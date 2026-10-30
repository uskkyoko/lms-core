package com.example.demo.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import java.util.Set;

public record CreateCourseRequest(
		@NotBlank(message = "Title is required.") @Size(max = 150) String title,
		@NotBlank(message = "Course code is required.") @Size(max = 30) @Pattern(regexp = "[ A-Za-z0-9-]+", message = "Use letters, numbers, spaces or hyphens for the course code.") String code,
		@NotBlank(message = "Description is required.") @Size(max = 2000) String description,
		@NotBlank(message = "Term is required.") @Size(max = 60) String term,
		@NotEmpty(message = "Assign at least one instructor.") Set<@NotNull @Positive Long> instructorIds) {
}
