package com.example.demo.web.dto.quiz;

import java.util.List;

public record EssayGradeRequest(List<EssayScoreRequest> answers) {
}
