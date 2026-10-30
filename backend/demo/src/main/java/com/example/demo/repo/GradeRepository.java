package com.example.demo.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Assignment;
import com.example.demo.domain.Grade;
import com.example.demo.domain.QuizAttempt;
import com.example.demo.domain.User;

public interface GradeRepository extends JpaRepository<Grade, Long> {

	Optional<Grade> findByAttempt(QuizAttempt attempt);

	Optional<Grade> findByStudentAndAssignment(User student, Assignment assignment);

	List<Grade> findByStudent(User student);
}
