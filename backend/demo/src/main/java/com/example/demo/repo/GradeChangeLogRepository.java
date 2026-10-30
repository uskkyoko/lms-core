package com.example.demo.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Grade;
import com.example.demo.domain.GradeChangeLog;

public interface GradeChangeLogRepository extends JpaRepository<GradeChangeLog, Long> {

	List<GradeChangeLog> findByGradeOrderByChangedAtDesc(Grade grade);

	List<GradeChangeLog> findAllByOrderByChangedAtDesc();
}
