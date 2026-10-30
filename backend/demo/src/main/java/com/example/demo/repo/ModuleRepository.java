package com.example.demo.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Course;
import com.example.demo.domain.Module;

public interface ModuleRepository extends JpaRepository<Module, Long> {

	List<Module> findByCourseOrderByOrderNumberAsc(Course course);
	void deleteByCourse(Course course);
}
