package com.example.demo.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Assignment;
import com.example.demo.domain.Submission;
import com.example.demo.domain.User;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

	Optional<Submission> findByAssignmentAndStudent(Assignment assignment, User student);

	List<Submission> findByAssignmentOrderBySubmittedAtAsc(Assignment assignment);

	List<Submission> findByAssignment(Assignment assignment);
}
