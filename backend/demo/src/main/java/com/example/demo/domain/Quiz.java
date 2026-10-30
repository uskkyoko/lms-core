package com.example.demo.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description = "";

    @Column(nullable = false)
    private Integer timeLimitMinutes; // FR-23: time limit

    @Column(nullable = false)
    private LocalDateTime openAt;  // FR-25: availability window

    @Column(nullable = false)
    private LocalDateTime closeAt; // FR-25

    @ManyToOne
    @JoinColumn(name = "module_id")
    private Module module;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    // Constructors
    public Quiz() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }
    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(Integer timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }
    public LocalDateTime getOpenAt() { return openAt; }
    public void setOpenAt(LocalDateTime openAt) { this.openAt = openAt; }
    public LocalDateTime getCloseAt() { return closeAt; }
    public void setCloseAt(LocalDateTime closeAt) { this.closeAt = closeAt; }
    public Module getModule() { return module; }
    public void setModule(Module module) { this.module = module; }
    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }

    // FR-25: check if quiz is available right now
    public boolean isAvailable() {
        return isAvailableAt(LocalDateTime.now());
    }

    public boolean isAvailableAt(LocalDateTime now) {
        return (now.isEqual(openAt) || now.isAfter(openAt)) && now.isBefore(closeAt);
    }

    public double totalPoints() {
        return questions.stream().mapToDouble(question -> question.getPoints() == null ? 0 : question.getPoints()).sum();
    }
}