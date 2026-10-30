package com.example.demo.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Assignment;
import com.example.demo.domain.Grade;
import com.example.demo.domain.Submission;
import com.example.demo.repo.AssignmentRepository;
import com.example.demo.repo.GradeRepository;
import com.example.demo.repo.SubmissionRepository;
import com.example.demo.service.LmsService;

@Component
@Transactional(readOnly = true)
public class AssignmentActivitySource implements CourseActivitySource {

	private final AssignmentRepository assignments;
	private final SubmissionRepository submissions;
	private final GradeRepository grades;
	private final LmsService lms;

	public AssignmentActivitySource(
			AssignmentRepository assignments,
			SubmissionRepository submissions,
			GradeRepository grades,
			LmsService lms) {
		this.assignments = assignments;
		this.submissions = submissions;
		this.grades = grades;
		this.lms = lms;
	}

	@Override
	public Set<Kind> supportedKinds() {
		return Set.of(Kind.ASSIGNMENT);
	}

	@Override
	public List<Activity> load(Long courseId, Set<Long> studentIds) {
		List<Activity> result = new ArrayList<>();
		for (Assignment assignment : assignments.findByModuleCourseOrderByDeadlineAscIdAsc(lms.requireCourse(courseId))) {
			if (!assignment.isPublished()) {
				continue;
			}
			List<Result> rows = new ArrayList<>();
			for (Submission submission : submissions.findByAssignment(assignment)) {
				if (!studentIds.contains(submission.getStudent().getId())) {
					continue;
				}
				Double points = grades.findByStudentAndAssignment(submission.getStudent(), assignment).map(Grade::getPoints).orElse(null);
				rows.add(new Result(submission.getStudent().getId(), true, points));
			}
			result.add(new Activity(Kind.ASSIGNMENT, assignment.getId(), assignment.getTitle(), assignment.getMaxPoints(), rows));
		}
		return result;
	}
}
