package com.example.demo.web.dto.quiz;

import java.time.LocalDateTime;
import java.util.List;

public record CreateQuizRequest(
		String title,
		String description,
		int timeLimitMinutes,
		LocalDateTime openAt,
		LocalDateTime closeAt,
		Long moduleId,
		List<QuestionRequest> questions) {
}
