package com.example.demo.web.dto.quiz;

import java.util.List;

import com.example.demo.domain.Question;

public record QuestionRequest(
		String text,
		Question.QuestionType type,
		double points,
		List<OptionRequest> options) {
}
