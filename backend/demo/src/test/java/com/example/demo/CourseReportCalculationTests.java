package com.example.demo;

import com.example.demo.domain.Course;
import com.example.demo.domain.User;
import com.example.demo.reporting.*;
import com.example.demo.service.LmsService;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.example.demo.reporting.CourseActivitySource.*;

/** Full aggregation contract for the material/submission modules, without introducing their entities here. */
class CourseReportCalculationTests {
    private CourseActivitySource source(Set<Kind> kinds, List<Activity> items) {
        return new CourseActivitySource() {
            public Set<Kind> supportedKinds() { return kinds; }
            public List<Activity> load(Long courseId, Set<Long> studentIds) { return items; }
        };
    }
    @Test void tc30AggregatesAllKindsAndKeepsZeroScoresDistinctFromPendingGrades() {
        LmsService lms = mock(LmsService.class); User student = mock(User.class); Course course = mock(Course.class);
        when(student.getId()).thenReturn(3L); when(lms.requireCourse(1L)).thenReturn(course);
        var source = source(EnumSet.allOf(Kind.class), List.of(
            new Activity(Kind.MATERIAL,1L,"Reading",null,List.of(new Result(3L,true,null))),
            new Activity(Kind.MATERIAL,2L,"Video",null,List.of()),
            new Activity(Kind.ASSIGNMENT,1L,"Submitted",50.0,List.of(new Result(3L,true,40.0))),
            new Activity(Kind.ASSIGNMENT,2L,"Missing",30.0,List.of()),
            new Activity(Kind.QUIZ,1L,"Zero score",10.0,List.of(new Result(3L,true,0.0))),
            new Activity(Kind.QUIZ,2L,"Essay pending",10.0,List.of(new Result(3L,true,null)))));
        var report = new CourseReportService(lms,List.of(source)).progress(student,1L);
        assertThat(report.completion().get(0).completed()).isEqualTo(1);
        assertThat(report.completion().get(0).percentage()).isEqualTo(50);
        assertThat(report.completion().get(1).completed()).isEqualTo(1);
        assertThat(report.completion().get(2).completed()).isEqualTo(2);
        assertThat(report.gradedAssessments()).isEqualTo(2);
        assertThat(report.pendingGrades()).isEqualTo(1);
        assertThat(report.currentGradePercentage()).isEqualTo(40.0);
        assertThat(report.unavailableSources()).isEmpty();
        verify(lms).requireEnrolledStudent(student,course);
    }
    @Test void invalidSourceCannotIncludeAnotherStudentsResults() {
        LmsService lms = mock(LmsService.class); User student = mock(User.class);
        when(student.getId()).thenReturn(3L); when(lms.requireCourse(1L)).thenReturn(mock(Course.class));
        var source = source(Set.of(Kind.ASSIGNMENT),List.of(new Activity(Kind.ASSIGNMENT,1L,"Task",10.0,List.of(new Result(4L,true,9.0)))));
        assertThatThrownBy(() -> new CourseReportService(lms,List.of(source)).progress(student,1L))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("Unexpected");
    }
    @Test void tc31CountsEachStudentOnceAndDoesNotAveragePendingWorkAsZero() {
        LmsService lms = mock(LmsService.class); User instructor = mock(User.class), first = mock(User.class), second = mock(User.class);
        when(first.getId()).thenReturn(3L); when(first.getFullName()).thenReturn("First");
        when(second.getId()).thenReturn(4L); when(second.getFullName()).thenReturn("Second");
        Course course = mock(Course.class); when(lms.requireCourse(1L)).thenReturn(course); when(lms.enrolledStudents(course)).thenReturn(List.of(first,second));
        var source = source(Set.of(Kind.ASSIGNMENT), List.of(
            new Activity(Kind.ASSIGNMENT,1L,"Task",10.0,List.of(new Result(3L,true,8.0),new Result(4L,true,null))),
            new Activity(Kind.ASSIGNMENT,2L,"Missing",20.0,List.of(new Result(3L,true,0.0)))));
        var report = new CourseReportService(lms,List.of(source)).analytics(instructor,1L);
        assertThat(report.assessments().get(0).submissionRate()).isEqualTo(100);
        assertThat(report.assessments().get(0).averageScore()).isEqualTo(8);
        assertThat(report.assessments().get(1).averageScore()).isEqualTo(0);
        assertThat(report.missingWork()).hasSize(1);
        assertThat(report.missingWork().get(0).studentId()).isEqualTo(4);
    }
}
