package com.example.demo.web.dto.content;

import java.time.LocalDateTime;

import com.example.demo.domain.Assignment;

public record AssignmentDto(
		Long id,
		Long courseId,
		Long moduleId,
		String title,
		String instructions,
		LocalDateTime deadline,
		double maxPoints,
		boolean published) {

	public static AssignmentDto from(Assignment assignment) {
		return new AssignmentDto(
				assignment.getId(),
				assignment.getModule().getCourse().getId(),
				assignment.getModule().getId(),
				assignment.getTitle(),
				assignment.getInstructions(),
				assignment.getDeadline(),
				assignment.getMaxPoints(),
				assignment.isPublished());
	}
}
