package com.example.demo.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Course;
import com.example.demo.domain.Material;
import com.example.demo.domain.MaterialCompletion;
import com.example.demo.domain.User;

public interface MaterialCompletionRepository extends JpaRepository<MaterialCompletion, Long> {

	Optional<MaterialCompletion> findByStudentAndMaterial(User student, Material material);

	List<MaterialCompletion> findByMaterialModuleCourse(Course course);

	List<MaterialCompletion> findByStudentAndMaterialModuleCourse(User student, Course course);

	void deleteByStudentAndMaterial(User student, Material material);

	void deleteByMaterialModuleCourse(Course course);
}
