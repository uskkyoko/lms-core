package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void roleSwitcherReturnsSampleUsers() throws Exception {
		mockMvc.perform(get("/api/users").param("role", "STUDENT"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].role").value("STUDENT"));
	}

	@Test
	void studentSeesOnlyEnrolledCourses() throws Exception {
		mockMvc.perform(get("/api/courses").header("X-User-Id", "4"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].code").value("SE331"));
	}

	@Test
	void administratorCanCreateCourseWithUniqueCode() throws Exception {
		mockMvc.perform(post("/api/courses")
						.header("X-User-Id", "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Databases","code":"DB201","description":"SQL and data modeling","term":"Fall 2026","instructorIds":[2]}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("DB201"));
	}

	@Test
	void duplicateCourseCodeIsRejected() throws Exception {
		mockMvc.perform(post("/api/courses")
						.header("X-User-Id", "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Software Engineering Copy","code":"SE331","description":"Duplicate","term":"Fall 2026","instructorIds":[2]}
								"""))
				.andExpect(status().isConflict());
	}

	@Test
	void studentCannotCreateCourse() throws Exception {
		mockMvc.perform(post("/api/courses")
						.header("X-User-Id", "4")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Hacked Course","code":"HX001","description":"Forbidden","term":"Fall 2026","instructorIds":[2]}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void studentCanStartOpenQuizAndResumeSavedAttempt() throws Exception {
		mockMvc.perform(post("/api/quizzes/1/start").header("X-User-Id", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.questions[0].type").value("MULTIPLE_CHOICE"));

		mockMvc.perform(post("/api/quizzes/1/start").header("X-User-Id", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"));
	}

	@Test
	void closedQuizCannotBeStarted() throws Exception {
		mockMvc.perform(post("/api/quizzes/3/start").header("X-User-Id", "3"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void studentCannotSeeGradebook() throws Exception {
		mockMvc.perform(get("/api/courses/1/gradebook").header("X-User-Id", "3"))
				.andExpect(status().isForbidden());
	}

	@Test
	void instructorGradebookShowsAllStudents() throws Exception {
		mockMvc.perform(get("/api/courses/1/gradebook").header("X-User-Id", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.students.length()").value(2))
				.andExpect(jsonPath("$.items.length()").value(3));
	}

	@Test
	void mixedQuizResultIsPendingUntilEssaysAreGraded() throws Exception {
		mockMvc.perform(get("/api/courses/1/grades").header("X-User-Id", "4"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.student.cells[0].status").value("PENDING_GRADING"));
	}

	@Test
	void gradeChangeIsLogged() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/grades/1")
						.header("X-User-Id", "2")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"points":18,"feedback":"Rechecked","reason":"Partial credit"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.points").value(18));

		mockMvc.perform(get("/api/courses/1/gradebook").header("X-User-Id", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.history[0].oldValue").value(20))
				.andExpect(jsonPath("$.history[0].newValue").value(18));
	}
}
