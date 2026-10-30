package com.example.demo.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.domain.Course;
import com.example.demo.domain.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

	List<Quiz> findByModuleId(Long moduleId);

	List<Quiz> findByModuleCourseOrderByTitleAsc(Course course);
	boolean existsByModuleCourse(Course course);

	@Query("select distinct q from Quiz q left join fetch q.questions where q.id = :id")
	Optional<Quiz> findWithQuestionsById(@Param("id") Long id);
}
