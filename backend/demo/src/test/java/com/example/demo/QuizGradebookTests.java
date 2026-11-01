package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.QuizAttempt;
import com.example.demo.repo.QuizAttemptRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuizGradebookTests {

	private static final String KSENIA = "3";
	private static final String ANDRII = "2";
	private static final String OLHA = "1";
	private static final String YELYZAVETA = "4";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private QuizAttemptRepository attempts;

	@Test
	void tc19AutoGradesMultipleChoiceAndShowsResult() throws Exception {
		long quizId = quizId("Week 1 Quiz");
		Map<String, Object> attempt = start(KSENIA, quizId);
		answerCorrect(KSENIA, attempt);
		mockMvc.perform(post("/api/attempts/" + id(attempt) + "/submit").header("X-User-Id", KSENIA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.score").value(20.0))
				.andExpect(jsonPath("$.revealAnswers").value(true));
	}

	@Test
	void tc20PendingUntilEssayGradedThenFinalResult() throws Exception {
		long quizId = quizId("Requirements Check");
		Map<String, Object> attempt = start(KSENIA, quizId);
		List<Map<String, Object>> questions = questions(attempt);
		save(KSENIA, id(attempt), id(questions.get(0)), optionId(questions.get(0), "Instructors and administrators"), null);
		MvcResult submitted = mockMvc.perform(post("/api/attempts/" + id(attempt) + "/submit").header("X-User-Id", KSENIA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDING_GRADING"))
				.andReturn();
		long attemptId = ((Number) JsonPath.read(submitted.getResponse().getContentAsString(), "$.id")).longValue();
		long essayId = ((Number) JsonPath.read(submitted.getResponse().getContentAsString(), "$.questions[1].id")).longValue();
		mockMvc.perform(post("/api/attempts/" + attemptId + "/essay-grades")
						.header("X-User-Id", ANDRII)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"answers\":[{\"questionId\":" + essayId + ",\"points\":8,\"feedback\":\"Clear\"}]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.score").value(18.0));
		mockMvc.perform(get("/api/attempts/" + attemptId).header("X-User-Id", KSENIA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.score").value(18.0));
	}

	@Test
	void tc21SecondAttemptIsBlocked() throws Exception {
		long quizId = quizId("Week 1 Quiz");
		Map<String, Object> attempt = start(KSENIA, quizId);
		answerCorrect(KSENIA, attempt);
		mockMvc.perform(post("/api/attempts/" + id(attempt) + "/submit").header("X-User-Id", KSENIA))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/quizzes/" + quizId + "/start").header("X-User-Id", KSENIA))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("No additional attempts are allowed. Each student has exactly one attempt."));
	}

	@Test
	void tc22QuizOutsideAvailabilityCannotStart() throws Exception {
		mockMvc.perform(post("/api/quizzes/" + quizId("Syllabus Warm-up") + "/start").header("X-User-Id", KSENIA))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("The quiz is unavailable. It is outside its availability window."));
	}

	@Test
	void tc23ServerEnforcesTimeLimitAndAutoSubmits() throws Exception {
		long quizId = quizId("Week 1 Quiz");
		Map<String, Object> started = start(KSENIA, quizId);
		QuizAttempt stored = attempts.findById(id(started)).orElseThrow();
		stored.setStartedAt(LocalDateTime.now().minusMinutes(stored.getQuiz().getTimeLimitMinutes() + 1));
		attempts.saveAndFlush(stored);
		mockMvc.perform(get("/api/attempts/" + stored.getId()).header("X-User-Id", KSENIA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.submittedAt").isNotEmpty());
	}

	@Test
	void tc24AutosaveRestoresAnswersAfterReconnect() throws Exception {
		long quizId = quizId("Week 1 Quiz");
		Map<String, Object> attempt = start(KSENIA, quizId);
		Map<String, Object> question = questions(attempt).get(0);
		long optionId = optionId(question, "Student, Instructor, and Administrator");
		save(KSENIA, id(attempt), id(question), optionId, null);
		mockMvc.perform(get("/api/attempts/" + id(attempt)).header("X-User-Id", KSENIA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.questions[0].selectedOptionId").value((int) optionId))
				.andExpect(jsonPath("$.questions[0].options[0].correct").doesNotExist());
	}

	@Test
	void tc25StudentSeesOnlyOwnGrades() throws Exception {
		mockMvc.perform(get("/api/courses/1/grades").header("X-User-Id", YELYZAVETA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.student.id").value(4))
				.andExpect(jsonPath("$.student.fullName").value("Yelyzaveta Maranda"))
				.andExpect(jsonPath("$.history.length()").value(0))
				.andExpect(jsonPath("$.students").doesNotExist());
		mockMvc.perform(get("/api/courses/1/gradebook").header("X-User-Id", YELYZAVETA))
				.andExpect(status().isForbidden());
	}

	@Test
	void tc26GradeChangeIsLoggedForInstructorAndAdmin() throws Exception {
		mockMvc.perform(put("/api/grades/1")
						.header("X-User-Id", ANDRII)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"points\":18,\"feedback\":\"Rechecked\",\"reason\":\"Partial credit\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.points").value(18.0));
		mockMvc.perform(get("/api/courses/1/gradebook").header("X-User-Id", ANDRII))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.history[0].oldValue").value(20.0))
				.andExpect(jsonPath("$.history[0].newValue").value(18.0))
				.andExpect(jsonPath("$.history[0].changedByName").value("Andrii Shevchenko"))
				.andExpect(jsonPath("$.history[0].changedByRole").value("INSTRUCTOR"));
		mockMvc.perform(get("/api/courses/1/gradebook").header("X-User-Id", OLHA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.history[0].changedByRole").value("INSTRUCTOR"));
		mockMvc.perform(put("/api/grades/1")
						.header("X-User-Id", KSENIA)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"points\":20}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/grades/1").header("X-User-Id", ANDRII))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(put("/api/grade-change-logs/1").header("X-User-Id", ANDRII).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/grade-change-logs/1").header("X-User-Id", ANDRII))
				.andExpect(status().isNotFound());
	}

	@Test
	void tc35StudentReviewsAnswersAfterGrading() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/courses/1/quizzes").header("X-User-Id", YELYZAVETA))
				.andExpect(status().isOk())
				.andReturn();
		List<Map<String, Object>> quizzes = JsonPath.read(result.getResponse().getContentAsString(), "$");
		long attemptId = 0;
		for (Map<String, Object> quiz : quizzes) {
			if ("Week 1 Quiz".equals(quiz.get("title"))) {
				Assertions.assertEquals("GRADED", quiz.get("studentStatus"));
				attemptId = ((Number) quiz.get("attemptId")).longValue();
			}
		}
		mockMvc.perform(get("/api/attempts/" + attemptId).header("X-User-Id", YELYZAVETA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.revealAnswers").value(true))
				.andExpect(jsonPath("$.score").value(20.0))
				.andExpect(jsonPath("$.questions[0].options[0].correct").exists());
	}

	@Test
	void tc36InstructorGradebookShowsAllStudentsAndItems() throws Exception {
		mockMvc.perform(get("/api/courses/1/gradebook").header("X-User-Id", ANDRII))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.students.length()").value(2))
				.andExpect(jsonPath("$.items.length()").value(3))
				.andExpect(jsonPath("$.students[0].totalPoints").exists())
				.andExpect(jsonPath("$.students[0].percentage").exists());
	}

	private long quizId(String title) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/courses/1/quizzes").header("X-User-Id", ANDRII))
				.andExpect(status().isOk())
				.andReturn();
		List<Map<String, Object>> quizzes = JsonPath.read(result.getResponse().getContentAsString(), "$");
		for (Map<String, Object> quiz : quizzes) {
			if (title.equals(quiz.get("title"))) {
				return ((Number) quiz.get("id")).longValue();
			}
		}
		throw new IllegalStateException("Quiz not found: " + title);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> start(String userId, long quizId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/quizzes/" + quizId + "/start").header("X-User-Id", userId))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$");
	}

	private void save(String userId, long attemptId, long questionId, Long optionId, String text) throws Exception {
		String body = optionId == null
				? "{\"questionId\":" + questionId + ",\"answerText\":\"" + text + "\"}"
				: "{\"questionId\":" + questionId + ",\"optionId\":" + optionId + "}";
		mockMvc.perform(post("/api/attempts/" + attemptId + "/answer")
						.header("X-User-Id", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk());
	}

	private void answerCorrect(String userId, Map<String, Object> attempt) throws Exception {
		List<Map<String, Object>> questions = questions(attempt);
		save(userId, id(attempt), id(questions.get(0)), optionId(questions.get(0), "Student, Instructor, and Administrator"), null);
		save(userId, id(attempt), id(questions.get(1)), optionId(questions.get(1), "Exactly one attempt"), null);
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> questions(Map<String, Object> attempt) {
		return (List<Map<String, Object>>) attempt.get("questions");
	}

	@SuppressWarnings("unchecked")
	private long optionId(Map<String, Object> question, String text) {
		List<Map<String, Object>> options = (List<Map<String, Object>>) question.get("options");
		for (Map<String, Object> option : options) {
			if (text.equals(option.get("text"))) {
				return ((Number) option.get("id")).longValue();
			}
		}
		throw new IllegalStateException("Option not found: " + text);
	}

	private long id(Map<String, Object> node) {
		return ((Number) node.get("id")).longValue();
	}
}
