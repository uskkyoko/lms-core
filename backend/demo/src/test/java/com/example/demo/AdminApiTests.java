package com.example.demo;

import com.example.demo.config.DemoDataLoader;
import com.example.demo.domain.*;
import com.example.demo.repo.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class AdminApiTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CourseRepository courses;
    @Autowired EnrollmentRepository enrollments;
    @Autowired ModuleRepository modules;
    @Autowired QuizAttemptRepository attempts;
    @Autowired DemoDataLoader seed;
    private String body(String code) { return """
        {"title":"Admin test","code":"%s","description":"Test description","term":"Fall 2026","instructorIds":[2]}
        """.formatted(code); }
    private long course(String code) { return courses.findByCodeIgnoreCase(code.strip()).orElseThrow().getId(); }
    private long create(String code) throws Exception {
        mvc.perform(post("/api/courses").header("X-User-Id",1).contentType(MediaType.APPLICATION_JSON).content(body(code))).andExpect(status().isCreated());
        return course(code);
    }
    @Test void tc01to03UsersHaveActualSubtypesAndServerResolvesSelectedIdentity() throws Exception {
        assertThat(users.findById(1L).orElseThrow()).isInstanceOf(Administrator.class);
        assertThat(users.findById(2L).orElseThrow()).isInstanceOf(Instructor.class);
        assertThat(users.findById(3L).orElseThrow()).isInstanceOf(Student.class);
        for (long id : new long[]{1,2,3,4,5})
            mvc.perform(get("/api/me").header("X-User-Id",id)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/courses")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/courses").header("X-User-Id",999999)).andExpect(status().isUnauthorized());
    }
    @Test void tc04to05RequiresFullDetailsInstructorAndNormalizedUniqueCode() throws Exception {
        create("  lower101  ");
        assertThat(courses.findByCodeIgnoreCase("LOWER101")).isPresent();
        mvc.perform(post("/api/courses").header("X-User-Id",1).contentType(MediaType.APPLICATION_JSON).content(body("lower101"))).andExpect(status().isConflict());
        for (String invalid : new String[]{"{}", body("INVALID").replace("Test description", "   "), body("INVALID").replace("[2]","[]"), body("INVALID").replace("[2]","[3]"), body("INVALID").replace("[2]","[null]"), body("INVALID").replace("[2]","[999999]")})
            mvc.perform(post("/api/courses").header("X-User-Id",1).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
    }
    @Test void tc06InstructorAssignmentUpdatesVisibilityAndRequiresInstructorRole() throws Exception {
        long id = create("PEOPLE1");
        Instructor other = users.save(new Instructor("Other instructor", "other-instructor@example.test"));
        String url = "/api/courses/"+id+"/instructors";
        mvc.perform(put(url).header("X-User-Id",1).contentType(MediaType.APPLICATION_JSON).content("{\"instructorIds\":["+other.getId()+"]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.instructors[0].id").value(other.getId()));
        mvc.perform(get("/api/courses").header("X-User-Id",other.getId())).andExpect(jsonPath("$[*].code",hasItem("PEOPLE1")));
        mvc.perform(get("/api/courses").header("X-User-Id",2)).andExpect(jsonPath("$[*].code",not(hasItem("PEOPLE1"))));
        mvc.perform(put(url).header("X-User-Id",1).contentType(MediaType.APPLICATION_JSON).content("{\"instructorIds\":[3]}" )).andExpect(status().isBadRequest());
        mvc.perform(put(url).header("X-User-Id",1).contentType(MediaType.APPLICATION_JSON).content("{\"instructorIds\":[]}" )).andExpect(status().isBadRequest());
    }
    @Test void tc06to08And33EnrollDuplicateRemoveAndReactivateKeepsHistory() throws Exception {
        long id = create("ENROLL1");
        String url = "/api/courses/"+id+"/enrollments";
        mvc.perform(post(url).header("X-User-Id",1).contentType(MediaType.APPLICATION_JSON).content("{\"studentId\":3}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.students[0].id").value(3));
        Enrollment enrollment = enrollments.findByCourseAndStudent(courses.findById(id).orElseThrow(),users.findById(3L).orElseThrow()).orElseThrow();
        Long originalId = enrollment.getId(); var originalDate = enrollment.getEnrollmentDate();
        mvc.perform(post(url).header("X-User-Id",1).contentType(MediaType.APPLICATION_JSON).content("{\"studentId\":3}" )).andExpect(status().isConflict());
        mvc.perform(get("/api/courses").header("X-User-Id",3)).andExpect(jsonPath("$[*].code",hasItem("ENROLL1")));
        mvc.perform(get("/api/courses").header("X-User-Id",5)).andExpect(jsonPath("$[*].code",not(hasItem("ENROLL1"))));
        mvc.perform(delete(url+"/3").header("X-User-Id",1)).andExpect(status().isOk()).andExpect(jsonPath("$.students",hasSize(0)));
        assertThat(enrollment.getStatus()).isEqualTo(Enrollment.Status.REMOVED);
        mvc.perform(get("/api/courses/"+id+"/progress").header("X-User-Id",3)).andExpect(status().isForbidden());
        mvc.perform(get("/api/courses").header("X-User-Id",3)).andExpect(jsonPath("$[*].code",not(hasItem("ENROLL1"))));
        mvc.perform(post(url).header("X-User-Id",1).contentType(MediaType.APPLICATION_JSON).content("{\"studentId\":3}" )).andExpect(status().isOk());
        assertThat(enrollment.getId()).isEqualTo(originalId);
        assertThat(enrollment.getEnrollmentDate()).isEqualTo(originalDate).isNotNull();
        assertThat(enrollment.getStatus()).isEqualTo(Enrollment.Status.ACTIVE);
    }
    @Test void tc32EditsArchivesAndDeletesCourseWithoutOrphaningModulesOrEnrollments() throws Exception {
        long id = create("CRUD1");
        Course course = courses.findById(id).orElseThrow();
        modules.save(new com.example.demo.domain.Module(course,"Empty section","No learning records",1));
        enrollments.save(new Enrollment(course,users.findById(3L).orElseThrow()));
        mvc.perform(put("/api/courses/"+id).header("X-User-Id",1).contentType(MediaType.APPLICATION_JSON).content(body("CRUD2"))).andExpect(status().isOk()).andExpect(jsonPath("$.code").value("CRUD2"));
        mvc.perform(put("/api/courses/"+id).header("X-User-Id",1).contentType(MediaType.APPLICATION_JSON).content(body("se331"))).andExpect(status().isConflict());
        mvc.perform(patch("/api/courses/"+id+"/archive").header("X-User-Id",1)).andExpect(jsonPath("$.status").value("ARCHIVED"));
        mvc.perform(delete("/api/courses/"+id).header("X-User-Id",1)).andExpect(status().isNoContent());
        assertThat(courses.existsById(id)).isFalse();
        assertThat(modules.findByCourseOrderByOrderNumberAsc(course)).isEmpty();
        assertThat(enrollments.findByCourseAndStudent(course,users.findById(3L).orElseThrow())).isEmpty();
    }
    @Test void courseWithAssessmentAndGradeHistoryMustBeArchived() throws Exception {
        mvc.perform(delete("/api/courses/"+course("SE331")).header("X-User-Id",1)).andExpect(status().isConflict());
        assertThat(courses.findByCodeIgnoreCase("SE331")).isPresent();
    }
    @Test void nfr02ForbidsAllAdminMutationsForOtherRoles() throws Exception {
        long id = course("SE331");
        for (long user : new long[]{2,3}) {
            mvc.perform(post("/api/courses").header("X-User-Id",user).contentType(MediaType.APPLICATION_JSON).content(body("FORBIDDEN"))).andExpect(status().isForbidden());
            mvc.perform(put("/api/courses/"+id).header("X-User-Id",user).contentType(MediaType.APPLICATION_JSON).content(body("FORBIDDEN"))).andExpect(status().isForbidden());
            mvc.perform(patch("/api/courses/"+id+"/archive").header("X-User-Id",user)).andExpect(status().isForbidden());
            mvc.perform(delete("/api/courses/"+id).header("X-User-Id",user)).andExpect(status().isForbidden());
            mvc.perform(delete("/api/courses/"+id+"/enrollments/3").header("X-User-Id",user)).andExpect(status().isForbidden());
            mvc.perform(post("/api/courses/"+id+"/enrollments").header("X-User-Id",user).contentType(MediaType.APPLICATION_JSON).content("{\"studentId\":5}" )).andExpect(status().isForbidden());
            mvc.perform(put("/api/courses/"+id+"/instructors").header("X-User-Id",user).contentType(MediaType.APPLICATION_JSON).content("{\"instructorIds\":[2]}" )).andExpect(status().isForbidden());
        }
    }
    @Test void removalAlsoBlocksExistingQuizAttemptWritesAndGrades() throws Exception {
        mvc.perform(post("/api/quizzes/1/start").header("X-User-Id",3)).andExpect(status().isOk());
        mvc.perform(delete("/api/courses/1/enrollments/3").header("X-User-Id",1)).andExpect(status().isOk());
        long attemptId = attempts.findByStudentIdAndQuizId(3L,1L).orElseThrow().getId();
        mvc.perform(post("/api/attempts/"+attemptId+"/answer").header("X-User-Id",3).contentType(MediaType.APPLICATION_JSON)
            .content("{\"questionId\":1,\"optionId\":1}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/attempts/"+attemptId+"/submit").header("X-User-Id",3)).andExpect(status().isForbidden());
        mvc.perform(get("/api/attempts/"+attemptId).header("X-User-Id",3)).andExpect(status().isForbidden());
        mvc.perform(get("/api/courses/1/grades").header("X-User-Id",3)).andExpect(status().isForbidden());
    }
    @Test void sampleDataIsNotDuplicatedOnSecondStartup() {
        long userCount = users.count(), courseCount = courses.count(), enrollmentCount = enrollments.count();
        seed.run();
        assertThat(users.count()).isEqualTo(userCount);
        assertThat(courses.count()).isEqualTo(courseCount);
        assertThat(enrollments.count()).isEqualTo(enrollmentCount);
    }
}
