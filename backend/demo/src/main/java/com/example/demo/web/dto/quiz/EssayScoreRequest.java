package com.example.demo.web.dto.quiz;

public record EssayScoreRequest(Long questionId, Double points, String feedback) {
}
