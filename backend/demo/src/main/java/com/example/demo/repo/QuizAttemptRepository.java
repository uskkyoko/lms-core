package com.example.demo.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.domain.Quiz;
import com.example.demo.domain.QuizAttempt;
import com.example.demo.domain.User;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

	Optional<QuizAttempt> findByStudentIdAndQuizId(Long studentId, Long quizId);

	List<QuizAttempt> findByQuizOrderByStudentFullNameAsc(Quiz quiz);

	@Query("""
			select distinct a from QuizAttempt a
			join fetch a.quiz
			join fetch a.student
			left join fetch a.answers
			where a.id = :id
			""")
	Optional<QuizAttempt> findWithDetailsById(@Param("id") Long id);
}
