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
@Table(name = "material_completions", uniqueConstraints = @UniqueConstraint(columnNames = { "student_id", "material_id" }))
public class MaterialCompletion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "student_id")
	private User student;

	@ManyToOne(optional = false)
	@JoinColumn(name = "material_id")
	private Material material;

	@Column(nullable = false)
	private LocalDateTime completedAt;

	protected MaterialCompletion() {
	}

	public MaterialCompletion(User student, Material material, LocalDateTime completedAt) {
		this.student = student;
		this.material = material;
		this.completedAt = completedAt;
	}

	public Long getId() {
		return id;
	}

	public User getStudent() {
		return student;
	}

	public Material getMaterial() {
		return material;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}
}
