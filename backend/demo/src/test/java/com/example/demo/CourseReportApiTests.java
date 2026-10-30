package com.example.demo;

import com.example.demo.domain.*;
import com.example.demo.repo.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class CourseReportApiTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired EnrollmentRepository enrollments;
    @Autowired CourseRepository courses;
    @Test void tc30ReportsRealQuizCompletionPendingEssaysAndGradebookTotal() throws Exception {
        mvc.perform(get("/api/courses/1/progress").header("X-User-Id",4)).andExpect(status().isOk())
            .andExpect(jsonPath("$.completion[2].kind").value("QUIZ")).andExpect(jsonPath("$.completion[2].completed").value(2))
            .andExpect(jsonPath("$.completion[2].total").value(3)).andExpect(jsonPath("$.earnedPoints").value(20))
            .andExpect(jsonPath("$.possiblePoints").value(45)).andExpect(jsonPath("$.currentGradePercentage").value(44.4))
            .andExpect(jsonPath("$.pendingGrades").value(1)).andExpect(jsonPath("$.unavailableSources",contains("MATERIAL","ASSIGNMENT")));
        mvc.perform(get("/api/courses/1/grades").header("X-User-Id",4)).andExpect(jsonPath("$.student.percentage").value(44.4));
    }
    @Test void tc31CalculatesRatesAveragesAndStudentsWithMissingWork() throws Exception {
        mvc.perform(get("/api/courses/1/analytics").header("X-User-Id",2)).andExpect(status().isOk())
            .andExpect(jsonPath("$.enrolledStudents").value(2))
            .andExpect(jsonPath("$.assessments[?(@.title == 'Week 1 Quiz')].submissionRate",contains(50.0)))
            .andExpect(jsonPath("$.assessments[?(@.title == 'Week 1 Quiz')].averageScore",contains(20.0)))
            .andExpect(jsonPath("$.assessments[?(@.title == 'Requirements Check')].gradedCount",contains(0)))
            .andExpect(jsonPath("$.missingWork",hasSize(4)));
    }
    @Test void instructorGradeChangesAppearInProgressAndAnalytics() throws Exception {
        mvc.perform(put("/api/grades/1").header("X-User-Id",2).contentType(MediaType.APPLICATION_JSON)
            .content("{\"points\":18,\"reason\":\"Rechecked\",\"feedback\":\"Updated\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/courses/1/progress").header("X-User-Id",4)).andExpect(jsonPath("$.earnedPoints").value(18)).andExpect(jsonPath("$.currentGradePercentage").value(40));
        mvc.perform(get("/api/courses/1/analytics").header("X-User-Id",2))
            .andExpect(jsonPath("$.assessments[?(@.title == 'Week 1 Quiz')].averageScore",contains(18.0)));
    }
    @Test void zeroActivityAndEmptyRostersDoNotInventScoresOrDivideByZero() throws Exception {
        mvc.perform(get("/api/courses/2/progress").header("X-User-Id",5)).andExpect(status().isOk())
            .andExpect(jsonPath("$.completion[*].completed",everyItem(is(0)))).andExpect(jsonPath("$.currentGradePercentage").isEmpty());
        for (long student : new long[]{3,4}) mvc.perform(delete("/api/courses/1/enrollments/"+student).header("X-User-Id",1)).andExpect(status().isOk());
        mvc.perform(get("/api/courses/1/analytics").header("X-User-Id",2))
            .andExpect(jsonPath("$.enrolledStudents").value(0)).andExpect(jsonPath("$.missingWork",hasSize(0)))
            .andExpect(jsonPath("$.assessments[*].submissionRate",everyItem(is(0.0))));
    }
    @Test void nfr02ReportsAreScopedToEnrolledStudentOrAssignedInstructor() throws Exception {
        mvc.perform(get("/api/courses/1/progress").header("X-User-Id",5).param("studentId","4")).andExpect(status().isForbidden());
        mvc.perform(get("/api/courses/1/progress").header("X-User-Id",2)).andExpect(status().isForbidden());
        mvc.perform(get("/api/courses/1/analytics").header("X-User-Id",3)).andExpect(status().isForbidden());
        mvc.perform(get("/api/courses/1/analytics").header("X-User-Id",1)).andExpect(status().isForbidden());
        Instructor other = users.save(new Instructor("Unassigned teacher","unassigned@example.test"));
        mvc.perform(get("/api/courses/1/analytics").header("X-User-Id",other.getId())).andExpect(status().isForbidden());
        mvc.perform(get("/api/courses/1/progress")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/courses/1/progress").header("X-User-Id",3))
            .andExpect(jsonPath("$.earnedPoints").value(0)).andExpect(jsonPath("$.completion[2].completed").value(0));
    }
}
