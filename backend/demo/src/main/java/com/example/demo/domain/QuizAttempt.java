package com.example.demo.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_attempts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "quiz_id"})) // FR-24: one attempt
public class QuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Double score;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL)
    private List<StudentAnswer> answers = new ArrayList<>();

    public enum Status {
        IN_PROGRESS, PENDING_GRADING, COMPLETED  // FR-22
    }

    public QuizAttempt() {}

    // Getters, setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }
    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public List<StudentAnswer> getAnswers() { return answers; }
    public void setAnswers(List<StudentAnswer> answers) { this.answers = answers; }
}