package com.example.demo.reporting;

import com.example.demo.domain.*;
import com.example.demo.service.LmsService;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.example.demo.reporting.CourseActivitySource.*;

@Service
@Transactional(readOnly = true)
public class CourseReportService {
    private final LmsService lms;
    private final List<CourseActivitySource> sources;
    public CourseReportService(LmsService lms, List<CourseActivitySource> sources) { this.lms = lms; this.sources = sources; }

    public StudentProgress progress(User actor, Long courseId) {
        Course course = lms.requireCourse(courseId); lms.requireEnrolledStudent(actor, course);
        Snapshot snapshot = load(courseId, Set.of(actor.getId()));
        List<Completion> completion = new ArrayList<>();
        double earned = 0, possible = 0; int pending = 0, graded = 0;
        for (Kind kind : Kind.values()) {
            List<Activity> activities = snapshot.activities().stream().filter(a -> a.kind() == kind).toList();
            int completed = 0;
            for (Activity activity : activities) {
                Result result = result(activity, actor.getId());
                if (kind != Kind.MATERIAL) possible += activity.maximumPoints();
                if (result != null && result.completed()) {
                    completed++;
                    if (kind != Kind.MATERIAL) {
                        if (result.points() == null) pending++;
                        else { earned += result.points(); graded++; }
                    }
                }
            }
            completion.add(new Completion(kind, completed, activities.size(), percent(completed, activities.size())));
        }
        // Same denominator and one-decimal rounding as the existing gradebook: all assessment items.
        Double grade = possible == 0 ? null : Math.round(earned * 1000.0 / possible) / 10.0;
        return new StudentProgress(courseId, course.getTitle(), completion, earned, possible, grade, graded, pending, snapshot.unavailable());
    }
    public InstructorAnalytics analytics(User actor, Long courseId) {
        Course course = lms.requireCourse(courseId); lms.requireAssignedInstructor(actor, course);
        List<User> students = lms.enrolledStudents(course);
        Set<Long> ids = new LinkedHashSet<>(); students.forEach(s -> ids.add(s.getId()));
        Snapshot snapshot = load(courseId, ids);
        List<Assessment> assessments = new ArrayList<>(); List<MissingWork> missing = new ArrayList<>();
        for (Activity activity : snapshot.activities()) {
            if (activity.kind() == Kind.MATERIAL) continue;
            int submitted = 0, graded = 0; double total = 0;
            for (User student : students) {
                Result result = result(activity, student.getId());
                if (result == null || !result.completed()) missing.add(new MissingWork(student.getId(), student.getFullName(), activity.kind(), activity.id(), activity.title()));
                else { submitted++; if (result.points() != null) { graded++; total += result.points(); } }
            }
            assessments.add(new Assessment(activity.kind(), activity.id(), activity.title(), activity.maximumPoints(), submitted,
                percent(submitted, students.size()), graded, graded == 0 ? null : round(total / graded)));
        }
        return new InstructorAnalytics(courseId, course.getTitle(), students.size(), assessments, missing, snapshot.unavailable());
    }
    private Snapshot load(Long courseId, Set<Long> studentIds) {
        Set<Kind> supported = EnumSet.noneOf(Kind.class); Set<String> keys = new HashSet<>(); List<Activity> activities = new ArrayList<>();
        for (CourseActivitySource source : sources) {
            Set<Kind> kinds = source.supportedKinds();
            if (!Collections.disjoint(supported, kinds)) throw new IllegalStateException("Each activity kind must have exactly one reporting source.");
            supported.addAll(kinds);
            for (Activity a : source.load(courseId, Set.copyOf(studentIds))) {
                if (!kinds.contains(a.kind()) || !keys.add(a.kind() + ":" + a.id())) throw new IllegalStateException("Invalid or duplicate activity.");
                if (a.kind() != Kind.MATERIAL && (a.maximumPoints() == null || !Double.isFinite(a.maximumPoints()) || a.maximumPoints() <= 0))
                    throw new IllegalStateException("Assessment maximum points must be positive and finite.");
                Set<Long> seen = new HashSet<>();
                for (Result r : a.results()) {
                    if (!studentIds.contains(r.studentId()) || !seen.add(r.studentId())) throw new IllegalStateException("Unexpected or duplicate student result.");
                    if (r.points() != null && (!Double.isFinite(r.points()) || !r.completed() || a.kind() == Kind.MATERIAL || r.points() < 0 || r.points() > a.maximumPoints()))
                        throw new IllegalStateException("Invalid assessment score.");
                }
                activities.add(a);
            }
        }
        return new Snapshot(activities, Arrays.stream(Kind.values()).filter(k -> !supported.contains(k)).toList());
    }
    private Result result(Activity activity, Long studentId) { return activity.results().stream().filter(r -> r.studentId().equals(studentId)).findFirst().orElse(null); }
    private static double round(double value) { return Math.round(value * 100.0) / 100.0; }
    private static double percent(int n, int d) { return d == 0 ? 0 : round(n * 100.0 / d); }
    private record Snapshot(List<Activity> activities, List<Kind> unavailable) {}
    public record Completion(Kind kind, int completed, int total, double percentage) {}
    public record StudentProgress(Long courseId, String courseTitle, List<Completion> completion, double earnedPoints, double possiblePoints,
        Double currentGradePercentage, int gradedAssessments, int pendingGrades, List<Kind> unavailableSources) {}
    public record Assessment(Kind kind, Long id, String title, double maximumPoints, int submitted, double submissionRate, int gradedCount, Double averageScore) {}
    public record MissingWork(Long studentId, String studentName, Kind kind, Long activityId, String activityTitle) {}
    public record InstructorAnalytics(Long courseId, String courseTitle, int enrolledStudents, List<Assessment> assessments, List<MissingWork> missingWork, List<Kind> unavailableSources) {}
}
