package com.example.demo;

import com.example.demo.course.CourseRepository;
import com.example.demo.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrototypeApiTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CourseRepository courses;

    private User user(Role role) { return users.findAll().stream().filter(user -> user.getRole() == role).findFirst().orElseThrow(); }
    private MockHttpSession select(Role role) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(put("/api/session").session(session).contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":" + user(role).getId() + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value(role.name()));
        return session;
    }
    private String body(String code) {
        return """
            {"title":"Prototype course","code":"%s","description":"A test course","term":"Fall 2026","instructorIds":[%d]}
            """.formatted(code, user(Role.INSTRUCTOR).getId());
    }
    @Test void switchesAllThreeRolesAndKeepsSessionsIndependent() throws Exception {
        for (Role role : Role.values()) {
            MockHttpSession session = select(role);
            mvc.perform(get("/api/session").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(user(role).getId()));
            mvc.perform(get("/api/users").param("role", role.name())).andExpect(status().isOk()).andExpect(jsonPath("$[*].role", everyItem(is(role.name()))));
        }
        MockHttpSession first = select(Role.STUDENT);
        select(Role.ADMINISTRATOR);
        mvc.perform(get("/api/session").session(first)).andExpect(jsonPath("$.role").value("STUDENT"));
        mvc.perform(get("/api/session").session(new MockHttpSession())).andExpect(status().isUnauthorized());
    }
    @Test void rejectsMissingOrUnknownSampleUserWithoutChangingSession() throws Exception {
        MockHttpSession session = select(Role.STUDENT);
        mvc.perform(put("/api/session").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"userId\":999999}"))
            .andExpect(status().isNotFound());
        mvc.perform(put("/api/session").session(session).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/session").session(session)).andExpect(jsonPath("$.role").value("STUDENT"));
    }
    @Test void createsCourseAndRejectsDuplicateCodeCaseInsensitively() throws Exception {
        MockHttpSession session = select(Role.ADMINISTRATOR);
        mvc.perform(post("/api/courses").session(session).contentType(MediaType.APPLICATION_JSON).content(body("test101")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("TEST101"))
            .andExpect(jsonPath("$.status").value("ACTIVE")).andExpect(jsonPath("$.instructors[0].id").value(user(Role.INSTRUCTOR).getId()));
        mvc.perform(get("/api/courses").session(session)).andExpect(jsonPath("$[*].code", hasItem("TEST101")));
        mvc.perform(post("/api/courses").session(session).contentType(MediaType.APPLICATION_JSON).content(body("TEST101")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.message", containsString("already exists")));
    }
    @Test void validatesRequiredFieldsAndInstructorRole() throws Exception {
        MockHttpSession session = select(Role.ADMINISTRATOR);
        long before = courses.count();
        mvc.perform(post("/api/courses").session(session).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.title").exists());
        mvc.perform(post("/api/courses").session(session).contentType(MediaType.APPLICATION_JSON).content(body("VALID1").replace("Prototype course", "   ")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/courses").session(session).contentType(MediaType.APPLICATION_JSON).content(body("VALID1").replace("[" + user(Role.INSTRUCTOR).getId() + "]", "[]")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/courses").session(session).contentType(MediaType.APPLICATION_JSON).content(body("VALID1").replace("[" + user(Role.INSTRUCTOR).getId() + "]", "[" + user(Role.STUDENT).getId() + "]")))
            .andExpect(status().isBadRequest());
        assertThat(courses.count()).isEqualTo(before);
    }
    @Test void forbidsEveryCourseMutationForStudentsAndInstructors() throws Exception {
        Long id = courses.findAll().get(0).getId();
        for (Role role : new Role[] {Role.STUDENT, Role.INSTRUCTOR}) {
            MockHttpSession session = select(role);
            mvc.perform(post("/api/courses").session(session).contentType(MediaType.APPLICATION_JSON).content(body("FORBIDDEN"))).andExpect(status().isForbidden());
            mvc.perform(put("/api/courses/" + id).session(session).contentType(MediaType.APPLICATION_JSON).content(body("FORBIDDEN"))).andExpect(status().isForbidden());
            mvc.perform(patch("/api/courses/" + id + "/archive").session(session)).andExpect(status().isForbidden());
            mvc.perform(delete("/api/courses/" + id).session(session)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/courses")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(body("FORBIDDEN"))).andExpect(status().isUnauthorized());
    }
    @Test void filtersCoursesByEnrollmentAndInstructorAssignment() throws Exception {
        mvc.perform(get("/api/courses").session(select(Role.STUDENT))).andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].code").value("SDT213"));
        mvc.perform(get("/api/courses").session(select(Role.INSTRUCTOR))).andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].code").value("SDT213"));
        mvc.perform(get("/api/courses").session(select(Role.ADMINISTRATOR))).andExpect(jsonPath("$", hasSize(2)));
        Student newcomer = users.save(new Student("New learner", "new@example.test"));
        MockHttpSession session = new MockHttpSession();
        mvc.perform(put("/api/session").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"userId\":" + newcomer.getId() + "}"));
        mvc.perform(get("/api/courses").session(session)).andExpect(jsonPath("$", hasSize(0)));
    }
    @Test void editsArchivesAndDeletesCourseWithEnrollments() throws Exception {
        MockHttpSession session = select(Role.ADMINISTRATOR);
        Long id = courses.findAll().stream().filter(course -> course.getCode().equals("SDT213")).findFirst().orElseThrow().getId();
        mvc.perform(put("/api/courses/" + id).session(session).contentType(MediaType.APPLICATION_JSON).content(body("SDT213")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Prototype course"));
        mvc.perform(put("/api/courses/" + id).session(session).contentType(MediaType.APPLICATION_JSON).content(body("SDT205")))
            .andExpect(status().isConflict());
        mvc.perform(patch("/api/courses/" + id + "/archive").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED"));
        mvc.perform(delete("/api/courses/" + id).session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/courses").session(select(Role.STUDENT))).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(delete("/api/courses/" + id).session(session)).andExpect(status().isNotFound());
    }
}
