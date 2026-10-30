package com.example.demo.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import java.time.LocalDate;

@Entity
@Table(
		name = "enrollments",
		uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "student_id"}))
public class Enrollment {
	public enum Status { ACTIVE, REMOVED }
	// Null is allowed for legacy rows whose date was never recorded.
	private LocalDate enrollmentDate = LocalDate.now();
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "varchar(20) default 'ACTIVE'")
	private Status status = Status.ACTIVE;

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
		if (student.getRole() != Role.STUDENT) throw new IllegalArgumentException("Only students can be enrolled.");
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
	public Status getStatus() { return status; }
	public LocalDate getEnrollmentDate() { return enrollmentDate; }
	public void remove() { status = Status.REMOVED; }
	public void reactivate() { status = Status.ACTIVE; }
}
