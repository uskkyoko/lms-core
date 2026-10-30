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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "submissions",
		uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "student_id"}))
public class Submission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "assignment_id")
	private Assignment assignment;

	@ManyToOne(optional = false)
	@JoinColumn(name = "student_id")
	private User student;

	@Column(nullable = false)
	private String storedFileName;

	@Column(nullable = false)
	private String originalFileName;

	private String contentType;

	@Column(nullable = false)
	private LocalDateTime submittedAt;

	protected Submission() {
	}

	public Submission(Assignment assignment, User student, String storedFileName, String originalFileName, String contentType, LocalDateTime submittedAt) {
		this.assignment = assignment;
		this.student = student;
		this.storedFileName = storedFileName;
		this.originalFileName = originalFileName;
		this.contentType = contentType;
		this.submittedAt = submittedAt;
	}

	public Long getId() {
		return id;
	}

	public Assignment getAssignment() {
		return assignment;
	}

	public User getStudent() {
		return student;
	}

	public String getStoredFileName() {
		return storedFileName;
	}

	public void setStoredFileName(String storedFileName) {
		this.storedFileName = storedFileName;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public void setOriginalFileName(String originalFileName) {
		this.originalFileName = originalFileName;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public LocalDateTime getSubmittedAt() {
		return submittedAt;
	}

	public void setSubmittedAt(LocalDateTime submittedAt) {
		this.submittedAt = submittedAt;
	}
}
