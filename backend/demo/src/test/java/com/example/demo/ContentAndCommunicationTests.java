package com.example.demo;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContentAndCommunicationTests {

	private static final String ADMIN = "1";
	private static final String INSTRUCTOR = "2";
	private static final String STUDENT = "3";
	private static final long SE331 = 1L;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void tc09InstructorCreatesCourseModule() throws Exception {
		mockMvc.perform(post("/api/courses/{courseId}/modules", SE331)
						.header("X-User-Id", INSTRUCTOR)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Week 2 – Requirements\",\"description\":\"Specification work\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Week 2 – Requirements"))
				.andExpect(jsonPath("$.courseId").value(1));
	}

	@Test
	void tc10StudentSeesOnlyPublishedMaterials() throws Exception {
		mockMvc.perform(get("/api/courses/{courseId}/modules", SE331).header("X-User-Id", STUDENT))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].materials[?(@.title=='Syllabus')]").isNotEmpty())
				.andExpect(jsonPath("$[0].materials[?(@.title=='Instructor notes')]").isEmpty());

		mockMvc.perform(get("/api/courses/{courseId}/modules", SE331).header("X-User-Id", INSTRUCTOR))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].materials[?(@.title=='Instructor notes')]").isNotEmpty());
	}

	@Test
	void tc11AllowedDocumentFormatsCanBeUploaded() throws Exception {
		long moduleId = firstModuleId();
		for (String name : new String[] {"notes.txt", "brief.docx", "slides.pdf"}) {
			mockMvc.perform(multipart("/api/modules/{moduleId}/materials/upload", moduleId)
							.file(file("file", name, "hello"))
							.param("title", name)
							.param("type", "FILE")
							.param("published", "true")
							.header("X-User-Id", INSTRUCTOR))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.originalFileName").value(name))
					.andExpect(jsonPath("$.type").value("FILE"));
		}
	}

	@Test
	void tc12OtherFileFormatsAreRejected() throws Exception {
		long moduleId = firstModuleId();
		mockMvc.perform(multipart("/api/modules/{moduleId}/materials/upload", moduleId)
						.file(file("file", "virus.exe", "binary"))
						.param("title", "Bad file")
						.param("type", "FILE")
						.header("X-User-Id", INSTRUCTOR))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("TXT, DOCX, or PDF")));
	}

	@Test
	void tc13InstructorCanCreateTextPage() throws Exception {
		long moduleId = firstModuleId();
		mockMvc.perform(post("/api/modules/{moduleId}/materials", moduleId)
						.header("X-User-Id", INSTRUCTOR)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Reading notes","type":"TEXT","body":"Read chapter 1.","published":true}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type").value("TEXT"))
				.andExpect(jsonPath("$.body").value("Read chapter 1."));
	}

	@Test
	void tc14InstructorCanAddVideoEmbed() throws Exception {
		long moduleId = firstModuleId();
		mockMvc.perform(post("/api/modules/{moduleId}/materials", moduleId)
						.header("X-User-Id", INSTRUCTOR)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Lecture 1","type":"VIDEO","embedUrl":"https://www.youtube.com/embed/8mAITcNt710","published":true}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type").value("VIDEO"))
				.andExpect(jsonPath("$.embedUrl").value("https://www.youtube.com/embed/8mAITcNt710"));
	}

	@Test
	void tc15MaterialsCanBePublishedAndHidden() throws Exception {
		long moduleId = firstModuleId();
		MvcResult created = mockMvc.perform(post("/api/modules/{moduleId}/materials", moduleId)
						.header("X-User-Id", INSTRUCTOR)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Draft page","type":"TEXT","body":"Not ready","published":false}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.published").value(false))
				.andReturn();
		long materialId = idOf(created);

		mockMvc.perform(patch("/api/materials/{id}/publish", materialId)
						.header("X-User-Id", INSTRUCTOR)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"published\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.published").value(true));

		mockMvc.perform(patch("/api/materials/{id}/publish", materialId)
						.header("X-User-Id", INSTRUCTOR)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"published\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.published").value(false));
	}

	@Test
	void tc16StudentCanDownloadPublishedFile() throws Exception {
		long moduleId = firstModuleId();
		MvcResult created = mockMvc.perform(multipart("/api/modules/{moduleId}/materials/upload", moduleId)
						.file(file("file", "guide.txt", "download-me"))
						.param("title", "Study guide")
						.param("type", "FILE")
						.param("published", "true")
						.header("X-User-Id", INSTRUCTOR))
				.andExpect(status().isOk())
				.andReturn();
		long materialId = idOf(created);

		mockMvc.perform(get("/api/materials/{id}/file", materialId).header("X-User-Id", STUDENT))
				.andExpect(status().isOk())
				.andExpect(content().string("download-me"));
	}

	@Test
	void tc17InstructorCreatesAssignment() throws Exception {
		String deadline = LocalDateTime.now().plusDays(7).toString();
		mockMvc.perform(post("/api/courses/{courseId}/assignments", SE331)
						.header("X-User-Id", INSTRUCTOR)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Lab 2","instructions":"Write a short report","deadline":"%s","maxPoints":50,"published":true}
								""".formatted(deadline)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Lab 2"))
				.andExpect(jsonPath("$.maxPoints").value(50));
	}

	@Test
	void tc18StudentCanSubmitBeforeDeadline() throws Exception {
		long assignmentId = createOpenAssignment("Open lab");
		mockMvc.perform(multipart("/api/assignments/{id}/submissions", assignmentId)
						.file(file("file", "work.txt", "my work"))
						.header("X-User-Id", STUDENT))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.originalFileName").value("work.txt"))
				.andExpect(jsonPath("$.student.id").value(3));
	}

	@Test
	void tc27StudentCanReplaceSubmissionBeforeDeadline() throws Exception {
		long assignmentId = createOpenAssignment("Replaceable lab");
		mockMvc.perform(multipart("/api/assignments/{id}/submissions", assignmentId)
						.file(file("file", "draft.txt", "draft"))
						.header("X-User-Id", STUDENT))
				.andExpect(status().isOk());

		mockMvc.perform(multipart("/api/assignments/{id}/submissions", assignmentId)
						.file(file("file", "final.txt", "final"))
						.header("X-User-Id", STUDENT))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.originalFileName").value("final.txt"));
	}

	@Test
	void tc28SubmissionAfterDeadlineIsBlocked() throws Exception {
		String deadline = LocalDateTime.now().minusHours(1).toString();
		MvcResult created = mockMvc.perform(post("/api/courses/{courseId}/assignments", SE331)
						.header("X-User-Id", INSTRUCTOR)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Closed lab","instructions":"Too late","deadline":"%s","maxPoints":100,"published":true}
								""".formatted(deadline)))
				.andExpect(status().isOk())
				.andReturn();
		long assignmentId = idOf(created);

		mockMvc.perform(multipart("/api/assignments/{id}/submissions", assignmentId)
						.file(file("file", "late.txt", "late"))
						.header("X-User-Id", STUDENT))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("deadline has passed")));
	}

	@Test
	void tc29InstructorGradesSubmissionWithFeedback() throws Exception {
		long assignmentId = createOpenAssignment("Graded lab");
		MvcResult submitted = mockMvc.perform(multipart("/api/assignments/{id}/submissions", assignmentId)
						.file(file("file", "essay.txt", "essay"))
						.header("X-User-Id", STUDENT))
				.andExpect(status().isOk())
				.andReturn();
		long submissionId = idOf(submitted);

		mockMvc.perform(post("/api/submissions/{id}/grade", submissionId)
						.header("X-User-Id", INSTRUCTOR)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"points\":88,\"feedback\":\"Clear structure. Add citations.\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.grade.points").value(88))
				.andExpect(jsonPath("$.grade.feedback").value("Clear structure. Add citations."));

		mockMvc.perform(get("/api/courses/{courseId}/gradebook", SE331).header("X-User-Id", INSTRUCTOR))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[?(@.title=='Graded lab')].type", hasItem("ASSIGNMENT")));
	}

	@Test
	void tc34AnnouncementsAndDiscussionPermissions() throws Exception {
		mockMvc.perform(post("/api/courses/{courseId}/announcements", SE331)
						.header("X-User-Id", INSTRUCTOR)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Office hours\",\"body\":\"Thursday 14:00.\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Office hours"));

		mockMvc.perform(post("/api/courses/{courseId}/announcements", SE331)
						.header("X-User-Id", STUDENT)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Nope\",\"body\":\"Students cannot announce.\"}"))
				.andExpect(status().isForbidden());

		MvcResult post = mockMvc.perform(post("/api/courses/{courseId}/discussions", SE331)
						.header("X-User-Id", STUDENT)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Question about lab\",\"body\":\"Is the PDF required?\"}"))
				.andExpect(status().isOk())
				.andReturn();
		long postId = idOf(post);

		mockMvc.perform(post("/api/discussions/{id}/replies", postId)
						.header("X-User-Id", INSTRUCTOR)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"body\":\"Yes, PDF is preferred.\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body").value("Yes, PDF is preferred."));

		mockMvc.perform(put("/api/discussions/{id}", postId)
						.header("X-User-Id", STUDENT)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Updated question\",\"body\":\"Is DOCX also accepted?\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body").value("Is DOCX also accepted?"));

		mockMvc.perform(put("/api/discussions/{id}", postId)
						.header("X-User-Id", ADMIN)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"body\":\"Admin should not edit someone else's post.\"}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete("/api/discussions/{id}", postId).header("X-User-Id", INSTRUCTOR))
				.andExpect(status().isOk());
	}

	private long firstModuleId() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/courses/{courseId}/modules", SE331).header("X-User-Id", INSTRUCTOR))
				.andExpect(status().isOk())
				.andReturn();
		return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$[0].id")).longValue();
	}

	private long createOpenAssignment(String title) throws Exception {
		String deadline = LocalDateTime.now().plusDays(2).toString();
		MvcResult created = mockMvc.perform(post("/api/courses/{courseId}/assignments", SE331)
						.header("X-User-Id", INSTRUCTOR)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"%s","instructions":"Submit a file","deadline":"%s","maxPoints":100,"published":true}
								""".formatted(title, deadline)))
				.andExpect(status().isOk())
				.andReturn();
		return idOf(created);
	}

	private long idOf(MvcResult result) throws Exception {
		return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
	}

	private MockMultipartFile file(String field, String filename, String body) {
		return new MockMultipartFile(field, filename, "text/plain", body.getBytes());
	}
}
