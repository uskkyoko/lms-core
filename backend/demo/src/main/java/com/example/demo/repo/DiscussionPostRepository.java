package com.example.demo.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Course;
import com.example.demo.domain.DiscussionPost;

public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, Long> {

	List<DiscussionPost> findByCourseAndParentIsNullOrderByCreatedAtDesc(Course course);

	void deleteByCourseAndParentIsNotNull(Course course);

	void deleteByCourse(Course course);
}
