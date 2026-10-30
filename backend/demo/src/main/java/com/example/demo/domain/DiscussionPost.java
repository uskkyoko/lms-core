package com.example.demo.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "discussion_posts")
public class DiscussionPost {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "course_id")
	private Course course;

	@ManyToOne
	@JoinColumn(name = "parent_id")
	private DiscussionPost parent;

	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("createdAt ASC")
	private List<DiscussionPost> replies = new ArrayList<>();

	private String title;

	@Column(nullable = false, length = 4000)
	private String body;

	@ManyToOne(optional = false)
	@JoinColumn(name = "author_id")
	private User author;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	protected DiscussionPost() {
	}

	public DiscussionPost(Course course, DiscussionPost parent, String title, String body, User author, LocalDateTime createdAt) {
		this.course = course;
		this.parent = parent;
		this.title = title;
		this.body = body;
		this.author = author;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Course getCourse() {
		return course;
	}

	public DiscussionPost getParent() {
		return parent;
	}

	public List<DiscussionPost> getReplies() {
		return replies;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public User getAuthor() {
		return author;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public boolean isReply() {
		return parent != null;
	}
}
