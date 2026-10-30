package com.example.demo.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.demo.domain.Course;
import com.example.demo.domain.Enrollment;
import com.example.demo.domain.Grade;
import com.example.demo.domain.Module;
import com.example.demo.domain.Question;
import com.example.demo.domain.QuestionOption;
import com.example.demo.domain.Quiz;
import com.example.demo.domain.QuizAttempt;
import com.example.demo.domain.Role;
import com.example.demo.domain.StudentAnswer;
import com.example.demo.domain.User;
import com.example.demo.domain.Student;
import com.example.demo.domain.Instructor;
import com.example.demo.domain.Administrator;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.repo.CourseRepository;
import com.example.demo.repo.EnrollmentRepository;
import com.example.demo.repo.GradeRepository;
import com.example.demo.repo.ModuleRepository;
import com.example.demo.repo.QuizAttemptRepository;
import com.example.demo.repo.QuizRepository;
import com.example.demo.repo.StudentAnswerRepository;
import com.example.demo.repo.UserRepository;

@Component
public class DemoDataLoader implements CommandLineRunner {

	private final UserRepository users;
	private final CourseRepository courses;
	private final EnrollmentRepository enrollments;
	private final ModuleRepository modules;
	private final QuizRepository quizzes;
	private final QuizAttemptRepository attempts;
	private final StudentAnswerRepository answers;
	private final GradeRepository grades;

	public DemoDataLoader(
			UserRepository users,
			CourseRepository courses,
			EnrollmentRepository enrollments,
			ModuleRepository modules,
			QuizRepository quizzes,
			QuizAttemptRepository attempts,
			StudentAnswerRepository answers,
			GradeRepository grades) {
		this.users = users;
		this.courses = courses;
		this.enrollments = enrollments;
		this.modules = modules;
		this.quizzes = quizzes;
		this.attempts = attempts;
		this.answers = answers;
		this.grades = grades;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (users.count() > 0) return;
		users.save(new Administrator("Olha Kovalenko", "olha.kovalenko@auk.edu.ua"));
		User instructor = users.save(new Instructor("Andrii Shevchenko", "andrii.shevchenko@auk.edu.ua"));
		User ksenia = users.save(new Student("Ksenia Maranda", "ksenia.maranda@auk.edu.ua"));
		User yelyzaveta = users.save(new Student("Yelyzaveta Maranda", "yelyzaveta.maranda@auk.edu.ua"));
		User uliana = users.save(new Student("Uliana Sova", "uliana.sova@auk.edu.ua"));

		Course se331 = new Course(
				"Software Engineering",
				"SE331",
				"Teaching and learning core of the university LMS: courses, materials, assignments, and quizzes.",
				"Fall 2026");
		se331.getInstructors().add(instructor);
		courses.save(se331);
		enrollments.save(new Enrollment(se331, ksenia));
		enrollments.save(new Enrollment(se331, yelyzaveta));

		Course ai101 = new Course(
				"Introduction to Artificial Intelligence",
				"AI101",
				"Foundations of AI for undergraduate students.",
				"Fall 2026");
		ai101.getInstructors().add(instructor);
		courses.save(ai101);
		enrollments.save(new Enrollment(ai101, uliana));

		Module week1 = modules.save(new Module(se331, "Week 1", "Requirements and LMS core", 1));
		LocalDateTime now = LocalDateTime.now();

		Quiz mcQuiz = quiz(week1, "Week 1 Quiz", "Multiple-choice check. Graded automatically.", 10,
				now.minusDays(7), now.plusDays(21));
		mcQuestion(mcQuiz, "Which roles does LMS Core support?", 10,
				"Student, Instructor, and Administrator", true,
				"Only Student and Instructor", false,
				"Guest visitors only", false);
		mcQuestion(mcQuiz, "How many quiz attempts does each student get?", 10,
				"Unlimited attempts", false,
				"Exactly one attempt", true,
				"Three attempts", false);
		quizzes.save(mcQuiz);

		Quiz mixed = quiz(week1, "Requirements Check", "Multiple-choice plus a manual essay.", 15,
				now.minusDays(1), now.plusDays(14));
		mcQuestion(mixed, "Who can change grades and see the change history?", 10,
				"Students in the same course", false,
				"Instructors and administrators", true,
				"Nobody after the deadline", false);
		Question essay = new Question();
		essay.setQuiz(mixed);
		essay.setText("Explain why quiz time limits must be enforced by the server, not only in the browser.");
		essay.setType(Question.QuestionType.ESSAY);
		essay.setPoints(10.0);
		mixed.getQuestions().add(essay);
		quizzes.save(mixed);

		Quiz closed = quiz(week1, "Syllabus Warm-up", "This quiz is outside its availability window.", 5,
				now.minusDays(20), now.minusDays(10));
		mcQuestion(closed, "What is the course code for Software Engineering?", 5,
				"SE331", true, "AI101", false, "DB201", false);
		quizzes.save(closed);

		seedGraded(mcQuiz, yelyzaveta, now.minusDays(2));
		seedPending(mixed, yelyzaveta, now.minusHours(3));
	}

	private Quiz quiz(Module module, String title, String description, int minutes, LocalDateTime open, LocalDateTime close) {
		Quiz quiz = new Quiz();
		quiz.setModule(module);
		quiz.setTitle(title);
		quiz.setDescription(description);
		quiz.setTimeLimitMinutes(minutes);
		quiz.setOpenAt(open);
		quiz.setCloseAt(close);
		return quiz;
	}

	private void mcQuestion(Quiz quiz, String text, double points, String a, boolean aOk, String b, boolean bOk, String c, boolean cOk) {
		Question question = new Question();
		question.setQuiz(quiz);
		question.setText(text);
		question.setType(Question.QuestionType.MULTIPLE_CHOICE);
		question.setPoints(points);
		addOption(question, a, aOk);
		addOption(question, b, bOk);
		addOption(question, c, cOk);
		quiz.getQuestions().add(question);
	}

	private void addOption(Question question, String text, boolean correct) {
		QuestionOption option = new QuestionOption();
		option.setQuestion(question);
		option.setText(text);
		option.setIsCorrect(correct);
		question.getOptions().add(option);
	}

	private void seedGraded(Quiz quiz, User student, LocalDateTime startedAt) {
		QuizAttempt attempt = new QuizAttempt();
		attempt.setQuiz(quiz);
		attempt.setStudent(student);
		attempt.setStartedAt(startedAt);
		attempt.setSubmittedAt(startedAt.plusMinutes(6));
		attempt.setStatus(QuizAttempt.Status.COMPLETED);
		attempt.setScore(quiz.totalPoints());
		attempts.save(attempt);
		double total = 0;
		for (Question question : quiz.getQuestions()) {
			StudentAnswer answer = new StudentAnswer();
			answer.setAttempt(attempt);
			answer.setQuestion(question);
			QuestionOption correct = question.getOptions().stream()
					.filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
					.findFirst()
					.orElseThrow();
			answer.setSelectedOption(correct);
			answer.setPointsAwarded(question.getPoints());
			answer.setSavedAt(startedAt.plusMinutes(2));
			answers.save(answer);
			total += question.getPoints();
		}
		Grade grade = new Grade();
		grade.setStudent(student);
		grade.setAttempt(attempt);
		grade.setPoints(total);
		grade.setMaxPoints(quiz.totalPoints());
		grade.setFeedback("Automatically graded.");
		grade.setGradedAt(attempt.getSubmittedAt());
		grades.save(grade);
	}

	private void seedPending(Quiz quiz, User student, LocalDateTime startedAt) {
		QuizAttempt attempt = new QuizAttempt();
		attempt.setQuiz(quiz);
		attempt.setStudent(student);
		attempt.setStartedAt(startedAt);
		attempt.setSubmittedAt(startedAt.plusMinutes(12));
		attempt.setStatus(QuizAttempt.Status.PENDING_GRADING);
		attempts.save(attempt);
		double score = 0;
		for (Question question : quiz.getQuestions()) {
			StudentAnswer answer = new StudentAnswer();
			answer.setAttempt(attempt);
			answer.setQuestion(question);
			if (question.getType() == Question.QuestionType.MULTIPLE_CHOICE) {
				QuestionOption correct = question.getOptions().stream()
						.filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
						.findFirst()
						.orElseThrow();
				answer.setSelectedOption(correct);
				answer.setPointsAwarded(question.getPoints());
				score += question.getPoints();
			} else {
				answer.setAnswerText(
						"The browser timer can be changed. The server must reject answers after the deadline.");
			}
			answer.setSavedAt(startedAt.plusMinutes(8));
			answers.save(answer);
		}
		attempt.setScore(score);
		attempts.save(attempt);
	}
}
