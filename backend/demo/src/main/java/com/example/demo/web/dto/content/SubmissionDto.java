package com.example.demo.web.dto.content;

import java.time.LocalDateTime;

import com.example.demo.domain.Submission;
import com.example.demo.web.dto.UserDto;

public record SubmissionDto(
		Long id,
		Long assignmentId,
		UserDto student,
		String originalFileName,
		LocalDateTime submittedAt,
		GradeDto grade) {

	public static SubmissionDto from(Submission submission, GradeDto grade) {
		return new SubmissionDto(
				submission.getId(),
				submission.getAssignment().getId(),
				UserDto.from(submission.getStudent()),
				submission.getOriginalFileName(),
				submission.getSubmittedAt(),
				grade);
	}
}
