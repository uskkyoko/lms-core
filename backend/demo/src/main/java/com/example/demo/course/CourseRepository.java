package com.example.demo.course;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CourseRepository extends JpaRepository<Course, Long> {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
    @Query("select c from Course c join c.instructors i where i.id = :userId order by c.id")
    List<Course> findAssigned(Long userId);
    @Query("select e.course from Enrollment e where e.student.id = :userId order by e.course.id")
    List<Course> findEnrolled(Long userId);
}
