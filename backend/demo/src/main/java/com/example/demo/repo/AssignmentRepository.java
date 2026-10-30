package com.example.demo.repo;

import com.example.demo.domain.Assignment;
import com.example.demo.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    boolean existsByModuleCourse(Course course);
}
