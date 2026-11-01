package com.example.demo.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Assignment;
import com.example.demo.domain.Course;
import com.example.demo.domain.Grade;
import com.example.demo.domain.GradeChangeLog;
import com.example.demo.domain.Module;
import com.example.demo.domain.Question;
import com.example.demo.domain.QuestionOption;
import com.example.demo.domain.Quiz;
import com.example.demo.domain.QuizAttempt;
import com.example.demo.domain.Role;
import com.example.demo.domain.StudentAnswer;
import com.example.demo.domain.Submission;
import com.example.demo.domain.User;
import com.example.demo.repo.AssignmentRepository;
import com.example.demo.repo.GradeChangeLogRepository;
import com.example.demo.repo.GradeRepository;
import com.example.demo.repo.ModuleRepository;
import com.example.demo.repo.QuizAttemptRepository;
import com.example.demo.repo.QuizRepository;
import com.example.demo.repo.StudentAnswerRepository;
import com.example.demo.repo.SubmissionRepository;
import com.example.demo.web.ForbiddenException;
import com.example.demo.web.dto.quiz.ChangeGradeRequest;
import com.example.demo.web.dto.quiz.CreateQuizRequest;
import com.example.demo.web.dto.quiz.EssayGradeRequest;
import com.example.demo.web.dto.quiz.EssayScoreRequest;
import com.example.demo.web.dto.quiz.OptionRequest;
import com.example.demo.web.dto.quiz.QuestionRequest;
import com.example.demo.web.dto.quiz.SaveAnswerRequest;

@Service
public class QuizService {

	private final LmsService lmsService;
	private final QuizRepository quizzes;
	private final QuizAttemptRepository attempts;
	private final StudentAnswerRepository answers;
	private final GradeRepository grades;
	private final GradeChangeLogRepository logs;
	private final ModuleRepository modules;
	private final AssignmentRepository assignments;
	private final SubmissionRepository submissions;

	public QuizService(
			LmsService lmsService,
			QuizRepository quizzes,
			QuizAttemptRepository attempts,
			StudentAnswerRepository answers,
			GradeRepository grades,
			GradeChangeLogRepository logs,
			ModuleRepository modules,
			AssignmentRepository assignments,
			SubmissionRepository submissions) {
		this.lmsService = lmsService;
		this.quizzes = quizzes;
		this.attempts = attempts;
		this.answers = answers;
		this.grades = grades;
		this.logs = logs;
		this.modules = modules;
		this.assignments = assignments;
		this.submissions = submissions;
	}

	@Transactional
	public List<Map<String, Object>> listQuizzes(User actor, Long courseId) {
		Course course = lmsService.requireCourse(courseId);
		lmsService.requireVisibleCourse(actor, course);
		LocalDateTime now = LocalDateTime.now();
		List<Map<String, Object>> result = new ArrayList<>();
		for (Quiz quiz : quizzes.findByModuleCourseOrderByTitleAsc(course)) {
			result.add(toQuizSummary(quiz, actor, now));
		}
		return result;
	}

	@Transactional
	public Map<String, Object> createQuiz(User actor, Long courseId, CreateQuizRequest request) {
		Course course = lmsService.requireCourse(courseId);
		lmsService.requireInstructorOrAdmin(actor, course);
		if (request.title() == null || request.title().isBlank()) {
			throw new IllegalArgumentException("Quiz title is required.");
		}
		if (request.timeLimitMinutes() < 1) {
			throw new IllegalArgumentException("Time limit must be at least 1 minute.");
		}
		if (request.openAt() == null || request.closeAt() == null || !request.closeAt().isAfter(request.openAt())) {
			throw new IllegalArgumentException("The quiz close time must be after the open time.");
		}
		if (request.questions() == null || request.questions().isEmpty()) {
			throw new IllegalArgumentException("A quiz must contain at least one question.");
		}
		Module module = resolveModule(course, request.moduleId());
		Quiz quiz = new Quiz();
		quiz.setTitle(request.title().trim());
		quiz.setDescription(request.description());
		quiz.setTimeLimitMinutes(request.timeLimitMinutes());
		quiz.setOpenAt(request.openAt());
		quiz.setCloseAt(request.closeAt());
		quiz.setModule(module);
		int index = 0;
		for (QuestionRequest item : request.questions()) {
			quiz.getQuestions().add(buildQuestion(quiz, item, index++));
		}
		Quiz saved = quizzes.save(quiz);
		return toQuizSummary(saved, actor, LocalDateTime.now());
	}

	@Transactional
	public Map<String, Object> startAttempt(User actor, Long quizId) {
		Quiz quiz = requireQuiz(quizId);
		lmsService.requireEnrolledStudent(actor, quiz.getModule().getCourse());
		LocalDateTime now = LocalDateTime.now();
		if (!quiz.isAvailableAt(now)) {
			throw new IllegalArgumentException("The quiz is unavailable. It is outside its availability window.");
		}
		QuizAttempt existing = attempts.findByStudentIdAndQuizId(actor.getId(), quizId).orElse(null);
		if (existing != null) {
			finalizeIfExpired(existing, now);
			if (existing.getStatus() == QuizAttempt.Status.IN_PROGRESS) {
				return toAttemptDto(existing, actor, now);
			}
			throw new IllegalArgumentException("No additional attempts are allowed. Each student has exactly one attempt.");
		}
		QuizAttempt attempt = new QuizAttempt();
		attempt.setStudent(actor);
		attempt.setQuiz(quiz);
		attempt.setStartedAt(now);
		attempt.setStatus(QuizAttempt.Status.IN_PROGRESS);
		return toAttemptDto(attempts.save(attempt), actor, now);
	}

	@Transactional
	public Map<String, Object> getAttempt(User actor, Long attemptId) {
		QuizAttempt attempt = requireAttempt(attemptId);
		assertCanViewAttempt(actor, attempt);
		LocalDateTime now = LocalDateTime.now();
		finalizeIfExpired(attempt, now);
		return toAttemptDto(attempt, actor, now);
	}

	@Transactional
	public Map<String, Object> saveAnswer(User actor, Long attemptId, SaveAnswerRequest request) {
		QuizAttempt attempt = requireAttempt(attemptId);
		assertOwnInProgress(actor, attempt);
		LocalDateTime now = LocalDateTime.now();
		if (finalizeIfExpired(attempt, now)) {
			throw new IllegalArgumentException("Time limit expired: the quiz was submitted automatically.");
		}
		Question question = questionOf(attempt.getQuiz(), request.questionId());
		StudentAnswer answer = answers.findByAttemptAndQuestion(attempt, question).orElseGet(() -> {
			StudentAnswer created = new StudentAnswer();
			created.setAttempt(attempt);
			created.setQuestion(question);
			attempt.getAnswers().add(created);
			return created;
		});
		if (question.getType() == Question.QuestionType.MULTIPLE_CHOICE) {
			if (request.optionId() == null) {
				answer.setSelectedOption(null);
			} else {
				QuestionOption option = question.getOptions().stream()
						.filter(item -> item.getId().equals(request.optionId()))
						.findFirst()
						.orElseThrow(() -> new IllegalArgumentException("Answer option not found."));
				answer.setSelectedOption(option);
			}
			answer.setAnswerText(null);
		} else {
			answer.setSelectedOption(null);
			answer.setAnswerText(request.answerText() == null ? "" : request.answerText());
		}
		answer.setSavedAt(now);
		answers.save(answer);
		return toAttemptDto(attempt, actor, now);
	}

	@Transactional
	public Map<String, Object> submitAttempt(User actor, Long attemptId) {
		QuizAttempt attempt = requireAttempt(attemptId);
		assertOwnInProgress(actor, attempt);
		return toAttemptDto(submit(attempt, LocalDateTime.now()), actor, LocalDateTime.now());
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listAttempts(User actor, Long quizId) {
		Quiz quiz = requireQuiz(quizId);
		lmsService.requireInstructorOrAdmin(actor, quiz.getModule().getCourse());
		LocalDateTime now = LocalDateTime.now();
		return attempts.findByQuizOrderByStudentFullNameAsc(quiz).stream()
				.map(attempt -> toAttemptDto(attempt, actor, now))
				.toList();
	}

	@Transactional
	public Map<String, Object> gradeEssays(User actor, Long attemptId, EssayGradeRequest request) {
		QuizAttempt attempt = requireAttempt(attemptId);
		lmsService.requireInstructorOrAdmin(actor, attempt.getQuiz().getModule().getCourse());
		if (attempt.getStatus() == QuizAttempt.Status.IN_PROGRESS) {
			throw new IllegalArgumentException("This attempt has not been submitted yet.");
		}
		if (request.answers() == null || request.answers().isEmpty()) {
			throw new IllegalArgumentException("At least one essay grade is required.");
		}
		for (EssayScoreRequest score : request.answers()) {
			Question question = questionOf(attempt.getQuiz(), score.questionId());
			if (question.getType() != Question.QuestionType.ESSAY) {
				throw new IllegalArgumentException("Only essay questions are graded manually.");
			}
			if (score.points() == null || score.points() < 0 || score.points() > question.getPoints()) {
				throw new IllegalArgumentException("Essay points cannot exceed the question value.");
			}
			StudentAnswer answer = answers.findByAttemptAndQuestion(attempt, question).orElseGet(() -> {
				StudentAnswer created = new StudentAnswer();
				created.setAttempt(attempt);
				created.setQuestion(question);
				attempt.getAnswers().add(created);
				return created;
			});
			answer.setPointsAwarded(score.points());
			answer.setFeedback(score.feedback());
			answers.save(answer);
		}
		recalculate(attempt, LocalDateTime.now());
		return toAttemptDto(attempt, actor, LocalDateTime.now());
	}

	@Transactional(readOnly = true)
	public Map<String, Object> gradebook(User actor, Long courseId) {
		Course course = lmsService.requireCourse(courseId);
		lmsService.requireInstructorOrAdmin(actor, course);
		return buildGradebook(course);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> studentGrades(User actor, Long courseId) {
		Course course = lmsService.requireCourse(courseId);
		lmsService.requireVisibleCourse(actor, course);
		if (actor.getRole() != Role.STUDENT) {
			throw new ForbiddenException("Instructors and administrators should use the gradebook.");
		}
		Map<String, Object> book = buildGradebook(course);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> students = (List<Map<String, Object>>) book.get("students");
		Map<String, Object> mine = students.stream()
				.filter(row -> Objects.equals(row.get("id"), actor.getId()))
				.findFirst()
				.orElse(Map.of("id", actor.getId(), "fullName", actor.getFullName(), "cells", List.of(), "totalPoints", 0, "maxPoints", 0, "percentage", 0));
		return Map.of(
				"items", book.get("items"),
				"student", mine,
				"history", List.of());
	}

	@Transactional
	public Map<String, Object> changeGrade(User actor, Long gradeId, ChangeGradeRequest request) {
		Grade grade = grades.findById(gradeId).orElseThrow(() -> new IllegalArgumentException("Grade not found."));
		Course course = grade.course();
		if (course == null) {
			throw new IllegalArgumentException("Grade is not linked to a course.");
		}
		lmsService.requireInstructorOrAdmin(actor, course);
		if (request.points() == null || request.points() < 0 || request.points() > grade.getMaxPoints()) {
			throw new IllegalArgumentException("Points cannot exceed the maximum for this item.");
		}
		double oldValue = grade.getPoints();
		grade.setPoints(request.points());
		grade.setFeedback(request.feedback());
		grade.setGradedAt(LocalDateTime.now());
		if (grade.getAttempt() != null) {
			grade.getAttempt().setScore(request.points());
		}
		if (Double.compare(oldValue, request.points()) != 0) {
			logs.save(new GradeChangeLog(grade, oldValue, request.points(), actor, request.reason()));
		}
		grades.save(grade);
		return Map.of(
				"id", grade.getId(),
				"points", grade.getPoints(),
				"maxPoints", grade.getMaxPoints(),
				"feedback", grade.getFeedback() == null ? "" : grade.getFeedback());
	}

	private Map<String, Object> buildGradebook(Course course) {
		List<Quiz> courseQuizzes = quizzes.findByModuleCourseOrderByTitleAsc(course);
		List<Assignment> courseAssignments = assignments.findByModuleCourseOrderByDeadlineAscIdAsc(course);
		List<Map<String, Object>> items = new ArrayList<>();
		for (Quiz quiz : courseQuizzes) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", quiz.getId());
			item.put("title", quiz.getTitle());
			item.put("maxPoints", quiz.totalPoints());
			item.put("type", "QUIZ");
			items.add(item);
		}
		for (Assignment assignment : courseAssignments) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", assignment.getId());
			item.put("title", assignment.getTitle());
			item.put("maxPoints", assignment.getMaxPoints());
			item.put("type", "ASSIGNMENT");
			items.add(item);
		}
		List<Map<String, Object>> students = new ArrayList<>();
		for (User student : lmsService.enrolledStudents(course)) {
			List<Map<String, Object>> cells = new ArrayList<>();
			double total = 0;
			double max = 0;
			for (Quiz quiz : courseQuizzes) {
				max += quiz.totalPoints();
				QuizAttempt attempt = attempts.findByStudentIdAndQuizId(student.getId(), quiz.getId()).orElse(null);
				Grade grade = attempt == null ? null : grades.findByAttempt(attempt).orElse(null);
				String status = cellStatus(attempt);
				Map<String, Object> cell = new LinkedHashMap<>();
				cell.put("type", "QUIZ");
				cell.put("quizId", quiz.getId());
				cell.put("gradeId", grade == null ? null : grade.getId());
				cell.put("status", status);
				cell.put("points", grade == null ? null : grade.getPoints());
				cell.put("maxPoints", quiz.totalPoints());
				cell.put("feedback", grade == null ? "" : grade.getFeedback());
				if (grade != null) {
					total += grade.getPoints();
				}
				cells.add(cell);
			}
			for (Assignment assignment : courseAssignments) {
				max += assignment.getMaxPoints();
				Submission submission = submissions.findByAssignmentAndStudent(assignment, student).orElse(null);
				Grade grade = grades.findByStudentAndAssignment(student, assignment).orElse(null);
				String status = grade != null ? "GRADED" : submission != null ? "PENDING_GRADING" : "MISSING";
				Map<String, Object> cell = new LinkedHashMap<>();
				cell.put("type", "ASSIGNMENT");
				cell.put("assignmentId", assignment.getId());
				cell.put("gradeId", grade == null ? null : grade.getId());
				cell.put("status", status);
				cell.put("points", grade == null ? null : grade.getPoints());
				cell.put("maxPoints", assignment.getMaxPoints());
				cell.put("feedback", grade == null ? "" : grade.getFeedback());
				if (grade != null) {
					total += grade.getPoints();
				}
				cells.add(cell);
			}
			double percentage = max == 0 ? 0 : Math.round(total * 1000.0 / max) / 10.0;
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", student.getId());
			row.put("fullName", student.getFullName());
			row.put("cells", cells);
			row.put("totalPoints", total);
			row.put("maxPoints", max);
			row.put("percentage", percentage);
			students.add(row);
		}
		List<Map<String, Object>> history = logs.findAllByOrderByChangedAtDesc().stream()
				.filter(log -> {
					Course logCourse = log.getGrade().course();
					return logCourse != null && logCourse.getId().equals(course.getId());
				})
				.map(this::toLog)
				.toList();
		Map<String, Object> book = new LinkedHashMap<>();
		book.put("items", items);
		book.put("students", students);
		book.put("history", history);
		return book;
	}

	private Module resolveModule(Course course, Long moduleId) {
		if (moduleId != null) {
			Module module = modules.findById(moduleId)
					.orElseThrow(() -> new IllegalArgumentException("Module not found."));
			if (module.getCourse() == null || !module.getCourse().getId().equals(course.getId())) {
				throw new IllegalArgumentException("The module does not belong to this course.");
			}
			return module;
		}
		return modules.findByCourseOrderByOrderNumberAsc(course).stream().findFirst()
				.orElseGet(() -> modules.save(new Module(course, "Course content", "Default module", 1)));
	}

	private Question buildQuestion(Quiz quiz, QuestionRequest request, int ignored) {
		if (request.text() == null || request.text().isBlank() || request.type() == null) {
			throw new IllegalArgumentException("Each question needs text and a type.");
		}
		Question question = new Question();
		question.setQuiz(quiz);
		question.setText(request.text().trim());
		question.setType(request.type());
		question.setPoints(request.points());
		if (request.type() == Question.QuestionType.MULTIPLE_CHOICE) {
			List<OptionRequest> options = request.options() == null ? List.of() : request.options();
			if (options.size() < 2) {
				throw new IllegalArgumentException("Multiple-choice questions need at least two options.");
			}
			long correct = options.stream().filter(OptionRequest::correct).count();
			if (correct != 1) {
				throw new IllegalArgumentException("Each multiple-choice question must have exactly one correct answer.");
			}
			for (OptionRequest optionRequest : options) {
				QuestionOption option = new QuestionOption();
				option.setQuestion(question);
				option.setText(optionRequest.text().trim());
				option.setIsCorrect(optionRequest.correct());
				question.getOptions().add(option);
			}
		}
		return question;
	}

	private boolean finalizeIfExpired(QuizAttempt attempt, LocalDateTime now) {
		if (attempt.getStatus() == QuizAttempt.Status.IN_PROGRESS && timeExpired(attempt, now)) {
			submit(attempt, deadline(attempt));
			return true;
		}
		return false;
	}

	private QuizAttempt submit(QuizAttempt attempt, LocalDateTime submittedAt) {
		if (attempt.getStatus() != QuizAttempt.Status.IN_PROGRESS) {
			return attempt;
		}
		attempt.setSubmittedAt(submittedAt);
		for (Question question : attempt.getQuiz().getQuestions()) {
			if (question.getType() != Question.QuestionType.MULTIPLE_CHOICE) {
				continue;
			}
			StudentAnswer answer = answers.findByAttemptAndQuestion(attempt, question).orElseGet(() -> {
				StudentAnswer created = new StudentAnswer();
				created.setAttempt(attempt);
				created.setQuestion(question);
				attempt.getAnswers().add(created);
				return created;
			});
			boolean correct = answer.getSelectedOption() != null
					&& Boolean.TRUE.equals(answer.getSelectedOption().getIsCorrect());
			answer.setPointsAwarded(correct ? question.getPoints() : 0.0);
			answers.save(answer);
		}
		recalculate(attempt, submittedAt);
		return attempt;
	}

	private void recalculate(QuizAttempt attempt, LocalDateTime at) {
		boolean pendingEssay = attempt.getQuiz().getQuestions().stream()
				.filter(question -> question.getType() == Question.QuestionType.ESSAY)
				.anyMatch(question -> answers.findByAttemptAndQuestion(attempt, question)
						.map(answer -> answer.getPointsAwarded() == null)
						.orElse(true));
		double score = attempt.getAnswers().stream()
				.map(StudentAnswer::getPointsAwarded)
				.filter(Objects::nonNull)
				.mapToDouble(Double::doubleValue)
				.sum();
		if (pendingEssay) {
			attempt.setStatus(QuizAttempt.Status.PENDING_GRADING);
			attempt.setScore(score);
			grades.findByAttempt(attempt).ifPresent(grades::delete);
			return;
		}
		attempt.setStatus(QuizAttempt.Status.COMPLETED);
		attempt.setScore(score);
		Grade grade = grades.findByAttempt(attempt).orElseGet(Grade::new);
		grade.setStudent(attempt.getStudent());
		grade.setAttempt(attempt);
		grade.setPoints(score);
		grade.setMaxPoints(attempt.getQuiz().totalPoints());
		grade.setFeedback(attempt.getAnswers().stream()
				.map(StudentAnswer::getFeedback)
				.filter(text -> text != null && !text.isBlank())
				.collect(Collectors.joining("\n")));
		grade.setGradedAt(at);
		grades.save(grade);
	}

	private Quiz requireQuiz(Long quizId) {
		return quizzes.findWithQuestionsById(quizId)
				.orElseThrow(() -> new IllegalArgumentException("Quiz not found."));
	}

	private QuizAttempt requireAttempt(Long attemptId) {
		QuizAttempt attempt = attempts.findWithDetailsById(attemptId)
				.orElseThrow(() -> new IllegalArgumentException("Quiz attempt not found."));
		requireQuiz(attempt.getQuiz().getId());
		return attempt;
	}

	private void assertCanViewAttempt(User actor, QuizAttempt attempt) {
		Course course = attempt.getQuiz().getModule().getCourse();
		if (actor.getRole() == Role.STUDENT) {
			if (!attempt.getStudent().getId().equals(actor.getId())) {
				throw new ForbiddenException("Students can only view their own quiz attempt.");
			}
			lmsService.requireVisibleCourse(actor, course);
			return;
		}
		lmsService.requireInstructorOrAdmin(actor, course);
	}

	private void assertOwnInProgress(User actor, QuizAttempt attempt) {
		lmsService.requireEnrolledStudent(actor, attempt.getQuiz().getModule().getCourse());
		if (actor.getRole() != Role.STUDENT || !attempt.getStudent().getId().equals(actor.getId())) {
			throw new ForbiddenException("Only the student who started this attempt can change answers.");
		}
		if (attempt.getStatus() != QuizAttempt.Status.IN_PROGRESS) {
			throw new IllegalArgumentException("This quiz has already been submitted.");
		}
	}

	private Question questionOf(Quiz quiz, Long questionId) {
		return quiz.getQuestions().stream()
				.filter(question -> question.getId().equals(questionId))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Question not found in this quiz."));
	}

	private LocalDateTime deadline(QuizAttempt attempt) {
		return attempt.getStartedAt().plusMinutes(attempt.getQuiz().getTimeLimitMinutes());
	}

	private boolean timeExpired(QuizAttempt attempt, LocalDateTime now) {
		return !now.isBefore(deadline(attempt));
	}

	private long remainingSeconds(QuizAttempt attempt, LocalDateTime now) {
		if (attempt.getStatus() != QuizAttempt.Status.IN_PROGRESS) {
			return 0;
		}
		return Math.max(0, Duration.between(now, deadline(attempt)).getSeconds());
	}

	private String availability(Quiz quiz, LocalDateTime now) {
		if (now.isBefore(quiz.getOpenAt())) {
			return "NOT_OPEN";
		}
		if (!now.isBefore(quiz.getCloseAt())) {
			return "CLOSED";
		}
		return "OPEN";
	}

	private String cellStatus(QuizAttempt attempt) {
		if (attempt == null) {
			return "MISSING";
		}
		return switch (attempt.getStatus()) {
			case IN_PROGRESS -> "IN_PROGRESS";
			case PENDING_GRADING -> "PENDING_GRADING";
			case COMPLETED -> "GRADED";
		};
	}

	private Map<String, Object> toQuizSummary(Quiz quiz, User actor, LocalDateTime now) {
		String availability = availability(quiz, now);
		String studentStatus = availability;
		Long attemptId = null;
		if (actor.getRole() == Role.STUDENT) {
			QuizAttempt attempt = attempts.findByStudentIdAndQuizId(actor.getId(), quiz.getId()).orElse(null);
			if (attempt != null) {
				finalizeIfExpired(attempt, now);
				attemptId = attempt.getId();
				studentStatus = cellStatus(attempt);
			} else if ("OPEN".equals(availability)) {
				studentStatus = "AVAILABLE";
			}
		}
		Map<String, Object> dto = new LinkedHashMap<>();
		dto.put("id", quiz.getId());
		dto.put("title", quiz.getTitle());
		dto.put("description", quiz.getDescription());
		dto.put("questionCount", quiz.getQuestions().size());
		dto.put("totalPoints", quiz.totalPoints());
		dto.put("timeLimitMinutes", quiz.getTimeLimitMinutes());
		dto.put("openAt", quiz.getOpenAt());
		dto.put("closeAt", quiz.getCloseAt());
		dto.put("availability", availability);
		dto.put("studentStatus", studentStatus);
		dto.put("attemptId", attemptId);
		return dto;
	}

	private Map<String, Object> toAttemptDto(QuizAttempt attempt, User actor, LocalDateTime now) {
		boolean reveal = attempt.getStatus() == QuizAttempt.Status.COMPLETED || actor.getRole() != Role.STUDENT;
		Map<Long, StudentAnswer> byQuestion = attempt.getAnswers().stream()
				.filter(answer -> answer.getQuestion() != null)
				.collect(Collectors.toMap(answer -> answer.getQuestion().getId(), answer -> answer, (a, b) -> a));
		List<Map<String, Object>> questions = attempt.getQuiz().getQuestions().stream().map(question -> {
			StudentAnswer answer = byQuestion.get(question.getId());
			List<Map<String, Object>> options = question.getOptions().stream().map(option -> {
				Map<String, Object> optionDto = new LinkedHashMap<>();
				optionDto.put("id", option.getId());
				optionDto.put("text", option.getText());
				if (reveal) {
					optionDto.put("correct", option.getIsCorrect());
				}
				return optionDto;
			}).toList();
			Map<String, Object> questionDto = new LinkedHashMap<>();
			questionDto.put("id", question.getId());
			questionDto.put("text", question.getText());
			questionDto.put("type", question.getType().name());
			questionDto.put("points", question.getPoints());
			questionDto.put("options", options);
			questionDto.put("selectedOptionId", answer == null || answer.getSelectedOption() == null ? null : answer.getSelectedOption().getId());
			questionDto.put("answerText", answer == null ? "" : answer.getAnswerText());
			questionDto.put("pointsAwarded", reveal && answer != null ? answer.getPointsAwarded() : null);
			questionDto.put("feedback", reveal && answer != null ? answer.getFeedback() : null);
			return questionDto;
		}).toList();
		Map<String, Object> dto = new LinkedHashMap<>();
		dto.put("id", attempt.getId());
		dto.put("quizId", attempt.getQuiz().getId());
		dto.put("quizTitle", attempt.getQuiz().getTitle());
		dto.put("student", Map.of("id", attempt.getStudent().getId(), "fullName", attempt.getStudent().getFullName()));
		dto.put("status", attempt.getStatus().name());
		dto.put("score", reveal ? attempt.getScore() : null);
		dto.put("maxPoints", attempt.getQuiz().totalPoints());
		dto.put("startedAt", attempt.getStartedAt());
		dto.put("submittedAt", attempt.getSubmittedAt());
		dto.put("remainingSeconds", remainingSeconds(attempt, now));
		dto.put("revealAnswers", reveal);
		dto.put("questions", questions);
		return dto;
	}

	private Map<String, Object> toLog(GradeChangeLog log) {
		Map<String, Object> dto = new LinkedHashMap<>();
		dto.put("id", log.getId());
		dto.put("gradeId", log.getGrade().getId());
		dto.put("itemTitle", log.getGrade().itemTitle());
		dto.put("studentName", log.getGrade().getStudent().getFullName());
		dto.put("oldValue", log.getOldValue());
		dto.put("newValue", log.getNewValue());
		dto.put("changedByName", log.getChangedByName());
		dto.put("changedByRole", log.getChangedByRole());
		dto.put("changedAt", log.getChangedAt());
		dto.put("reason", log.getReason());
		return dto;
	}
}
