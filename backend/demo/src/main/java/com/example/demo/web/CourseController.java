package com.example.demo.web;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.domain.User;
import com.example.demo.service.LmsService;
import com.example.demo.web.dto.CourseDto;
import com.example.demo.web.dto.CreateCourseRequest;
import com.example.demo.web.dto.EnrollRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "http://localhost:5173")
public class CourseController {

	private final LmsService lmsService;

	public CourseController(LmsService lmsService) {
		this.lmsService = lmsService;
	}

	@GetMapping
	public List<CourseDto> list(@RequestHeader("X-User-Id") Long userId) {
		User actor = lmsService.requireUser(userId);
		return lmsService.listCourses(actor);
	}

	@PostMapping
	public CourseDto create(
			@RequestHeader("X-User-Id") Long userId,
			@Valid @RequestBody CreateCourseRequest request) {
		User actor = lmsService.requireUser(userId);
		return lmsService.createCourse(actor, request);
	}

	@PostMapping("/{courseId}/enrollments")
	public CourseDto enroll(
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long courseId,
			@Valid @RequestBody EnrollRequest request) {
		User actor = lmsService.requireUser(userId);
		return lmsService.enrollStudent(actor, courseId, request.studentId());
	}
}
