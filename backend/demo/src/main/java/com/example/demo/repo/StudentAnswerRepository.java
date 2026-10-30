package com.example.demo.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Question;
import com.example.demo.domain.QuizAttempt;
import com.example.demo.domain.StudentAnswer;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {

	Optional<StudentAnswer> findByAttemptAndQuestion(QuizAttempt attempt, Question question);
}
