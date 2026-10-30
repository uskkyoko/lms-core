package com.example.demo.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Course;
import com.example.demo.domain.Enrollment;
import com.example.demo.domain.User;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

	boolean existsByCourseAndStudentAndStatus(Course course, User student, Enrollment.Status status);
	default boolean existsByCourseAndStudent(Course course, User student) {
		return existsByCourseAndStudentAndStatus(course, student, Enrollment.Status.ACTIVE);
	}
	Optional<Enrollment> findByCourseAndStudent(Course course, User student);
	void deleteByCourse(Course course);

	@Query("select e from Enrollment e where e.student = :student and e.status = com.example.demo.domain.Enrollment.Status.ACTIVE order by e.course.code")
	List<Enrollment> findByStudentOrderByCourseCodeAsc(User student);

	@Query("select e from Enrollment e where e.course = :course and e.status = com.example.demo.domain.Enrollment.Status.ACTIVE order by e.student.fullName")
	List<Enrollment> findByCourseOrderByStudentFullNameAsc(Course course);
}
