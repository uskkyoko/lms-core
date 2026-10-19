package com.example.demo.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "enrollments",
		uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "student_id"}))
public class Enrollment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "course_id")
	private Course course;

	@ManyToOne(optional = false)
	@JoinColumn(name = "student_id")
	private User student;

	protected Enrollment() {
	}

	public Enrollment(Course course, User student) {
		this.course = course;
		this.student = student;
	}

	public Long getId() {
		return id;
	}

	public Course getCourse() {
		return course;
	}

	public User getStudent() {
		return student;
	}
}
