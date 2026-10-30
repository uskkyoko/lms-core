package com.example.demo.web;

import com.example.demo.reporting.CourseReportService;
import com.example.demo.service.LmsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}")
public class CourseReportController {
    private final CourseReportService reports;
    private final LmsService lms;
    public CourseReportController(CourseReportService reports, LmsService lms) { this.reports = reports; this.lms = lms; }
    @GetMapping("/progress")
    public CourseReportService.StudentProgress progress(@RequestHeader(value = "X-User-Id", required = false) Long userId, @PathVariable Long courseId) {
        return reports.progress(lms.requireUser(userId), courseId);
    }
    @GetMapping("/analytics")
    public CourseReportService.InstructorAnalytics analytics(@RequestHeader(value = "X-User-Id", required = false) Long userId, @PathVariable Long courseId) {
        return reports.analytics(lms.requireUser(userId), courseId);
    }
}
