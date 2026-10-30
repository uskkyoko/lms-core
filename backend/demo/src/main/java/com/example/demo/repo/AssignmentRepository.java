package com.example.demo.repo;

import java.util.List;

import com.example.demo.domain.Assignment;
import com.example.demo.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    boolean existsByModuleCourse(Course course);

    List<Assignment> findByModuleCourseOrderByDeadlineAscIdAsc(Course course);
}
