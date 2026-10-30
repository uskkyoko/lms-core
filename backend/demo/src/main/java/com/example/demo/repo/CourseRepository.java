package com.example.demo.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Course;
import com.example.demo.domain.User;

public interface CourseRepository extends JpaRepository<Course, Long> {

	boolean existsByCodeIgnoreCase(String code);
	boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

	Optional<Course> findByCodeIgnoreCase(String code);

	List<Course> findByInstructorsContainingOrderByCodeAsc(User instructor);

	List<Course> findAllByOrderByCodeAsc();
}
