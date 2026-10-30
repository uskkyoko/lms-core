package com.example.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "materials")
public class Material {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "module_id")
	private Module module;

	@Column(nullable = false)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MaterialType type;

	@Column(length = 8000)
	private String body;

	private String embedUrl;

	private String storedFileName;

	private String originalFileName;

	private String contentType;

	@Column(nullable = false)
	private boolean published;

	@ManyToOne(optional = false)
	@JoinColumn(name = "created_by_id")
	private User createdBy;

	protected Material() {
	}

	public Material(Module module, String title, MaterialType type, boolean published, User createdBy) {
		this.module = module;
		this.title = title;
		this.type = type;
		this.published = published;
		this.createdBy = createdBy;
	}

	public Long getId() {
		return id;
	}

	public Module getModule() {
		return module;
	}

	public String getTitle() {
		return title;
	}

	public MaterialType getType() {
		return type;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getEmbedUrl() {
		return embedUrl;
	}

	public void setEmbedUrl(String embedUrl) {
		this.embedUrl = embedUrl;
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

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public boolean hasFile() {
		return storedFileName != null && !storedFileName.isBlank();
	}
}
