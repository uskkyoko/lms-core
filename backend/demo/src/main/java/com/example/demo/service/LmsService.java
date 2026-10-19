package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Course;
import com.example.demo.domain.Enrollment;
import com.example.demo.domain.Role;
import com.example.demo.domain.User;
import com.example.demo.repo.CourseRepository;
import com.example.demo.repo.EnrollmentRepository;
import com.example.demo.repo.UserRepository;
import com.example.demo.web.ForbiddenException;
import com.example.demo.web.dto.CourseDto;
import com.example.demo.web.dto.CreateCourseRequest;
import com.example.demo.web.dto.UserDto;

@Service
public class LmsService {

	private final UserRepository users;
	private final CourseRepository courses;
	private final EnrollmentRepository enrollments;

	public LmsService(UserRepository users, CourseRepository courses, EnrollmentRepository enrollments) {
		this.users = users;
		this.courses = courses;
		this.enrollments = enrollments;
	}

	public User requireUser(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("Select a sample user first.");
		}
		return users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
	}

	public List<UserDto> listUsers(Role role) {
		if (role == null) {
			return users.findAll().stream().map(UserDto::from).toList();
		}
		return users.findByRoleOrderByFullNameAsc(role).stream().map(UserDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<CourseDto> listCourses(User actor) {
		List<Course> visible = switch (actor.getRole()) {
			case ADMINISTRATOR -> courses.findAllByOrderByCodeAsc();
			case INSTRUCTOR -> courses.findByInstructorsContainingOrderByCodeAsc(actor);
			case STUDENT -> enrollments.findByStudentOrderByCourseCodeAsc(actor).stream()
					.map(Enrollment::getCourse)
					.toList();
		};
		return visible.stream().map(this::toDto).toList();
	}

	@Transactional
	public CourseDto createCourse(User actor, CreateCourseRequest request) {
		requireAdmin(actor);
		String code = request.code().trim().toUpperCase();
		if (courses.existsByCodeIgnoreCase(code)) {
			throw new IllegalArgumentException("Course code already exists. Choose a different unique course code.");
		}
		Course course = new Course(
				request.title().trim(),
				code,
				request.description() == null ? "" : request.description().trim(),
				request.term().trim());
		return toDto(courses.save(course));
	}

	@Transactional
	public CourseDto enrollStudent(User actor, Long courseId, Long studentId) {
		requireAdmin(actor);
		Course course = courses.findById(courseId)
				.orElseThrow(() -> new IllegalArgumentException("Course not found."));
		User student = users.findById(studentId)
				.orElseThrow(() -> new IllegalArgumentException("Student not found."));
		if (student.getRole() != Role.STUDENT) {
			throw new IllegalArgumentException("Only students can be enrolled in a course.");
		}
		if (enrollments.existsByCourseAndStudent(course, student)) {
			throw new IllegalArgumentException("This student is already enrolled in the course.");
		}
		enrollments.save(new Enrollment(course, student));
		return toDto(course);
	}

	private CourseDto toDto(Course course) {
		List<UserDto> students = enrollments.findByCourseOrderByStudentFullNameAsc(course).stream()
				.map(enrollment -> UserDto.from(enrollment.getStudent()))
				.toList();
		return CourseDto.from(course, students);
	}

	private void requireAdmin(User actor) {
		if (actor.getRole() != Role.ADMINISTRATOR) {
			throw new ForbiddenException("Only administrators can manage courses.");
		}
	}
}
