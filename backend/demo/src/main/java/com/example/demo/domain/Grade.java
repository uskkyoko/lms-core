package com.example.demo.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "grades")
public class Grade {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "student_id")
	private User student;

	@ManyToOne
	@JoinColumn(name = "assignment_id")
	private Assignment assignment;

	@ManyToOne
	@JoinColumn(name = "attempt_id")
	private QuizAttempt attempt;

	@Column(nullable = false)
	private Double points;

	@Column(nullable = false)
	private Double maxPoints;

	@Column(length = 2000)
	private String feedback = "";

	private LocalDateTime gradedAt;

	public Grade() {
	}

	public Long getId() {
		return id;
	}

	public User getStudent() {
		return student;
	}

	public void setStudent(User student) {
		this.student = student;
	}

	public Assignment getAssignment() {
		return assignment;
	}

	public void setAssignment(Assignment assignment) {
		this.assignment = assignment;
	}

	public QuizAttempt getAttempt() {
		return attempt;
	}

	public void setAttempt(QuizAttempt attempt) {
		this.attempt = attempt;
	}

	public Double getPoints() {
		return points;
	}

	public void setPoints(Double points) {
		this.points = points;
	}

	public Double getMaxPoints() {
		return maxPoints;
	}

	public void setMaxPoints(Double maxPoints) {
		this.maxPoints = maxPoints;
	}

	public String getFeedback() {
		return feedback;
	}

	public void setFeedback(String feedback) {
		this.feedback = feedback == null ? "" : feedback;
	}

	public LocalDateTime getGradedAt() {
		return gradedAt;
	}

	public void setGradedAt(LocalDateTime gradedAt) {
		this.gradedAt = gradedAt;
	}

	public String itemTitle() {
		if (attempt != null && attempt.getQuiz() != null) {
			return attempt.getQuiz().getTitle();
		}
		if (assignment != null) {
			return assignment.getTitle();
		}
		return "Graded item";
	}

	public Course course() {
		if (attempt != null && attempt.getQuiz() != null && attempt.getQuiz().getModule() != null) {
			return attempt.getQuiz().getModule().getCourse();
		}
		if (assignment != null && assignment.getModule() != null) {
			return assignment.getModule().getCourse();
		}
		return null;
	}
}
