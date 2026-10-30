package com.example.demo.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.domain.User;
import com.example.demo.service.LmsService;
import com.example.demo.service.QuizService;
import com.example.demo.web.dto.quiz.CreateQuizRequest;
import com.example.demo.web.dto.quiz.EssayGradeRequest;
import com.example.demo.web.dto.quiz.SaveAnswerRequest;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class QuizController {

	private final LmsService lmsService;
	private final QuizService quizService;

	public QuizController(LmsService lmsService, QuizService quizService) {
		this.lmsService = lmsService;
		this.quizService = quizService;
	}

	@GetMapping("/courses/{courseId}/quizzes")
	public List<Map<String, Object>> list(
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long courseId) {
		User actor = lmsService.requireUser(userId);
		return quizService.listQuizzes(actor, courseId);
	}

	@PostMapping("/courses/{courseId}/quizzes")
	public Map<String, Object> create(
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long courseId,
			@RequestBody CreateQuizRequest request) {
		User actor = lmsService.requireUser(userId);
		return quizService.createQuiz(actor, courseId, request);
	}

	@PostMapping("/quizzes/{quizId}/start")
	public Map<String, Object> start(
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long quizId) {
		User actor = lmsService.requireUser(userId);
		return quizService.startAttempt(actor, quizId);
	}

	@GetMapping("/quizzes/{quizId}/attempts")
	public List<Map<String, Object>> attempts(
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long quizId) {
		User actor = lmsService.requireUser(userId);
		return quizService.listAttempts(actor, quizId);
	}

	@GetMapping("/attempts/{attemptId}")
	public Map<String, Object> getAttempt(
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long attemptId) {
		User actor = lmsService.requireUser(userId);
		return quizService.getAttempt(actor, attemptId);
	}

	@PostMapping("/attempts/{attemptId}/answer")
	public Map<String, Object> saveAnswer(
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long attemptId,
			@RequestBody SaveAnswerRequest request) {
		User actor = lmsService.requireUser(userId);
		return quizService.saveAnswer(actor, attemptId, request);
	}

	@PostMapping("/attempts/{attemptId}/submit")
	public Map<String, Object> submit(
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long attemptId) {
		User actor = lmsService.requireUser(userId);
		return quizService.submitAttempt(actor, attemptId);
	}

	@PostMapping("/attempts/{attemptId}/essay-grades")
	public Map<String, Object> gradeEssays(
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long attemptId,
			@RequestBody EssayGradeRequest request) {
		User actor = lmsService.requireUser(userId);
		return quizService.gradeEssays(actor, attemptId, request);
	}
}
