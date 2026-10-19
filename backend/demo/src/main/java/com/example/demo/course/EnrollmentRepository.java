package com.example.demo.course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    void deleteByCourseId(Long courseId);
    void deleteByStudentId(Long studentId);
}
