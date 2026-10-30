package com.example.demo.web.dto.quiz;

public record SaveAnswerRequest(Long questionId, Long optionId, String answerText) {
}
