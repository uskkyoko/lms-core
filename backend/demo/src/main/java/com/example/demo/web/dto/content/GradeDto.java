package com.example.demo.web.dto.content;

import java.time.LocalDateTime;

import com.example.demo.domain.Grade;

public record GradeDto(Long id, double points, double maxPoints, String feedback, LocalDateTime gradedAt) {

	public static GradeDto from(Grade grade) {
		return new GradeDto(grade.getId(), grade.getPoints(), grade.getMaxPoints(), grade.getFeedback(), grade.getGradedAt());
	}
}
