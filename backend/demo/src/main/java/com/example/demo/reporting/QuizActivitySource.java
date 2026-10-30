package com.example.demo.reporting;

import com.example.demo.domain.Grade;
import com.example.demo.domain.Quiz;
import com.example.demo.domain.QuizAttempt;
import com.example.demo.repo.*;
import com.example.demo.service.LmsService;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class QuizActivitySource implements CourseActivitySource {
    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final GradeRepository grades;
    private final LmsService lms;
    public QuizActivitySource(QuizRepository quizzes, QuizAttemptRepository attempts, GradeRepository grades, LmsService lms) {
        this.quizzes = quizzes; this.attempts = attempts; this.grades = grades; this.lms = lms;
    }
    public Set<Kind> supportedKinds() { return Set.of(Kind.QUIZ); }
    public List<Activity> load(Long courseId, Set<Long> studentIds) {
        List<Activity> result = new ArrayList<>();
        for (Quiz quiz : quizzes.findByModuleCourseOrderByTitleAsc(lms.requireCourse(courseId))) {
            List<Result> results = new ArrayList<>();
            for (QuizAttempt attempt : attempts.findByQuizOrderByStudentFullNameAsc(quiz)) {
                if (!studentIds.contains(attempt.getStudent().getId())) continue;
                boolean completed = attempt.getStatus() != QuizAttempt.Status.IN_PROGRESS;
                // Grade is authoritative, including instructor changes; a partial essay score is not a final grade.
                Double points = completed ? grades.findByAttempt(attempt).map(Grade::getPoints).orElse(null) : null;
                results.add(new Result(attempt.getStudent().getId(), completed, points));
            }
            result.add(new Activity(Kind.QUIZ, quiz.getId(), quiz.getTitle(), quiz.totalPoints(), results));
        }
        return result;
    }
}
