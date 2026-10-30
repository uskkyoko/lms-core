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
@Table(name = "grade_change_logs")
public class GradeChangeLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "grade_id", updatable = false)
	private Grade grade;

	@Column(nullable = false, updatable = false)
	private Double oldValue;

	@Column(nullable = false, updatable = false)
	private Double newValue;

	@Column(nullable = false, updatable = false)
	private Long changedByUserId;

	@Column(nullable = false, updatable = false)
	private String changedByName;

	@Column(nullable = false, updatable = false)
	private String changedByRole;

	@Column(nullable = false, updatable = false)
	private LocalDateTime changedAt;

	@Column(length = 1000, updatable = false)
	private String reason = "";

	public GradeChangeLog() {
	}

	public GradeChangeLog(Grade grade, Double oldValue, Double newValue, User actor, String reason) {
		this.grade = grade;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.changedByUserId = actor.getId();
		this.changedByName = actor.getFullName();
		this.changedByRole = actor.getRole().name();
		this.changedAt = LocalDateTime.now();
		this.reason = reason == null ? "" : reason;
	}

	public Long getId() {
		return id;
	}

	public Grade getGrade() {
		return grade;
	}

	public Double getOldValue() {
		return oldValue;
	}

	public Double getNewValue() {
		return newValue;
	}

	public Long getChangedByUserId() {
		return changedByUserId;
	}

	public String getChangedByName() {
		return changedByName;
	}

	public String getChangedByRole() {
		return changedByRole;
	}

	public LocalDateTime getChangedAt() {
		return changedAt;
	}

	public String getReason() {
		return reason;
	}
}
