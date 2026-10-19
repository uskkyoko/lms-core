package com.example.demo.web.dto;

import java.util.List;

import com.example.demo.domain.Course;
import com.example.demo.domain.CourseStatus;

public record CourseDto(
		Long id,
		String title,
		String code,
		String description,
		String term,
		CourseStatus status,
		List<UserDto> instructors,
		List<UserDto> students) {

	public static CourseDto from(Course course, List<UserDto> students) {
		List<UserDto> instructors = course.getInstructors().stream()
				.map(UserDto::from)
				.sorted((a, b) -> a.fullName().compareToIgnoreCase(b.fullName()))
				.toList();
		return new CourseDto(
				course.getId(),
				course.getTitle(),
				course.getCode(),
				course.getDescription(),
				course.getTerm(),
				course.getStatus(),
				instructors,
				students);
	}
}
