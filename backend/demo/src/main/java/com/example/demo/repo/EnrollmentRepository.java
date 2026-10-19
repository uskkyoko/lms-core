package com.example.demo.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Course;
import com.example.demo.domain.Enrollment;
import com.example.demo.domain.User;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

	boolean existsByCourseAndStudent(Course course, User student);

	List<Enrollment> findByStudentOrderByCourseCodeAsc(User student);

	List<Enrollment> findByCourseOrderByStudentFullNameAsc(Course course);
}
