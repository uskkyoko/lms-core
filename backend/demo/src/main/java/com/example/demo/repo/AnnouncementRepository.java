package com.example.demo.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Announcement;
import com.example.demo.domain.Course;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

	List<Announcement> findByCourseOrderByCreatedAtDesc(Course course);

	void deleteByCourse(Course course);
}
