package com.example.demo.web;

import java.util.List;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import jakarta.validation.constraints.*;
import java.util.Set;

import com.example.demo.domain.User;
import com.example.demo.service.LmsService;
import com.example.demo.web.dto.CourseDto;
import com.example.demo.web.dto.CreateCourseRequest;
import com.example.demo.web.dto.EnrollRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/courses")

public class CourseController {

	private final LmsService lmsService;

	public CourseController(LmsService lmsService) {
		this.lmsService = lmsService;
	}

	@GetMapping
	public List<CourseDto> list(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
		User actor = lmsService.requireUser(userId);
		return lmsService.listCourses(actor);
	}

	@PostMapping @ResponseStatus(HttpStatus.CREATED)
	public CourseDto create(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@Valid @RequestBody CreateCourseRequest request) {
		User actor = lmsService.requireUser(userId);
		return lmsService.createCourse(actor, request);
	}

	@PostMapping("/{courseId}/enrollments")
	public CourseDto enroll(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long courseId,
			@Valid @RequestBody EnrollRequest request) {
		User actor = lmsService.requireUser(userId);
		return lmsService.enrollStudent(actor, courseId, request.studentId());
	}
	@PutMapping("/{courseId}")
	public CourseDto update(@RequestHeader(value = "X-User-Id", required = false) Long userId, @PathVariable Long courseId,
			@Valid @RequestBody CreateCourseRequest request) {
		return lmsService.updateCourse(lmsService.requireUser(userId), courseId, request);
	}
	@PatchMapping("/{courseId}/archive")
	public CourseDto archive(@RequestHeader(value = "X-User-Id", required = false) Long userId, @PathVariable Long courseId) {
		return lmsService.archiveCourse(lmsService.requireUser(userId), courseId);
	}
	@DeleteMapping("/{courseId}") @ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@RequestHeader(value = "X-User-Id", required = false) Long userId, @PathVariable Long courseId) {
		lmsService.deleteCourse(lmsService.requireUser(userId), courseId);
	}
	@PutMapping("/{courseId}/instructors")
	public CourseDto assign(@RequestHeader(value = "X-User-Id", required = false) Long userId, @PathVariable Long courseId,
			@Valid @RequestBody InstructorRequest request) {
		return lmsService.assignInstructors(lmsService.requireUser(userId), courseId, request.instructorIds());
	}
	@DeleteMapping("/{courseId}/enrollments/{studentId}")
	public CourseDto remove(@RequestHeader(value = "X-User-Id", required = false) Long userId, @PathVariable Long courseId, @PathVariable Long studentId) {
		return lmsService.removeStudent(lmsService.requireUser(userId), courseId, studentId);
	}
	public record InstructorRequest(@NotEmpty(message = "Assign at least one instructor.") Set<@NotNull @Positive Long> instructorIds) {}
}
