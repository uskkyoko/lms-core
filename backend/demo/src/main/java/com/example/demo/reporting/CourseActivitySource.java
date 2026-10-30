package com.example.demo.reporting;

import java.util.List;
import java.util.Set;

/** Implement in the owning module; reports read existing activity records, never duplicate them. */
public interface CourseActivitySource {
    enum Kind { MATERIAL, ASSIGNMENT, QUIZ }
    Set<Kind> supportedKinds();
    /** Visible activities for this course, including those with no results. Only requested student IDs may appear. */
    List<Activity> load(Long courseId, Set<Long> studentIds);
    /** completed means material marked complete, assignment submitted, or quiz submitted (including pending essays). */
    record Result(Long studentId, boolean completed, Double points) {}
    /** maximumPoints is positive for assessments, null for materials; null result points mean ungraded. */
    record Activity(Kind kind, Long id, String title, Double maximumPoints, List<Result> results) {}
}
