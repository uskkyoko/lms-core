package com.example.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.demo.domain.Course;
import com.example.demo.domain.Enrollment;
import com.example.demo.domain.Role;
import com.example.demo.domain.User;
import com.example.demo.repo.CourseRepository;
import com.example.demo.repo.EnrollmentRepository;
import com.example.demo.repo.UserRepository;

@Component
public class DemoDataLoader implements CommandLineRunner {

	private final UserRepository users;
	private final CourseRepository courses;
	private final EnrollmentRepository enrollments;

	public DemoDataLoader(UserRepository users, CourseRepository courses, EnrollmentRepository enrollments) {
		this.users = users;
		this.courses = courses;
		this.enrollments = enrollments;
	}

	@Override
	public void run(String... args) {
		users.save(new User("Olha Kovalenko", "olha.kovalenko@auk.edu.ua", Role.ADMINISTRATOR));
		User instructor = users.save(new User("Andrii Shevchenko", "andrii.shevchenko@auk.edu.ua", Role.INSTRUCTOR));
		User student = users.save(new User("Ksenia Maranda", "ksenia.maranda@auk.edu.ua", Role.STUDENT));

		Course se331 = new Course(
				"Software Engineering",
				"SE331",
				"Teaching and learning core of the university LMS: courses, materials, assignments, and quizzes.",
				"Fall 2026");
		se331.getInstructors().add(instructor);
		courses.save(se331);
		enrollments.save(new Enrollment(se331, student));

		Course ai101 = new Course(
				"Introduction to Artificial Intelligence",
				"AI101",
				"Foundations of AI for undergraduate students. Not yet assigned to an instructor.",
				"Fall 2026");
		courses.save(ai101);
		enrollments.save(new Enrollment(ai101, student));
	}
}
