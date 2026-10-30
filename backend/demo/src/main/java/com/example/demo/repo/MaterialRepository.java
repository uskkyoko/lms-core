package com.example.demo.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Course;
import com.example.demo.domain.Material;
import com.example.demo.domain.Module;

public interface MaterialRepository extends JpaRepository<Material, Long> {

	List<Material> findByModuleOrderByIdAsc(Module module);

	List<Material> findByModuleCourseAndPublishedTrueOrderByIdAsc(Course course);

	void deleteByModuleCourse(Course course);
}
