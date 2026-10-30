package com.example.demo.web;

import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.domain.User;
import com.example.demo.service.LmsService;
import com.example.demo.service.QuizService;
import com.example.demo.web.dto.quiz.ChangeGradeRequest;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class GradebookController {

	private final LmsService lmsService;
	private final QuizService quizService;

	public GradebookController(LmsService lmsService, QuizService quizService) {
		this.lmsService = lmsService;
		this.quizService = quizService;
	}

	@GetMapping("/courses/{courseId}/gradebook")
	public Map<String, Object> gradebook(
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long courseId) {
		User actor = lmsService.requireUser(userId);
		return quizService.gradebook(actor, courseId);
	}

	@GetMapping("/courses/{courseId}/grades")
	public Map<String, Object> myGrades(
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long courseId) {
		User actor = lmsService.requireUser(userId);
		return quizService.studentGrades(actor, courseId);
	}

	@PutMapping("/grades/{gradeId}")
	public Map<String, Object> updateGrade(
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long gradeId,
			@RequestBody ChangeGradeRequest request) {
		User actor = lmsService.requireUser(userId);
		return quizService.changeGrade(actor, gradeId, request);
	}
}
