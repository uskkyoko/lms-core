package com.example.demo.course;

import com.example.demo.user.Instructor;
import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "courses")
public class Course {
    public enum Status { ACTIVE, ARCHIVED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String title;
    @Column(nullable = false, unique = true, length = 30)
    private String code;
    @Column(nullable = false, length = 2000)
    private String description;
    @Column(nullable = false, length = 60)
    private String term;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Status status = Status.ACTIVE;
    @ManyToMany
    @JoinTable(name = "course_instructors", joinColumns = @JoinColumn(name = "course_id"), inverseJoinColumns = @JoinColumn(name = "instructor_id"))
    private Set<Instructor> instructors = new LinkedHashSet<>();

    protected Course() {}
    public Course(String title, String code, String description, String term, Set<Instructor> instructors) {
        update(title, code, description, term, instructors);
    }
    public void update(String title, String code, String description, String term, Set<Instructor> instructors) {
        this.title = title; this.code = code; this.description = description; this.term = term;
        this.instructors.clear(); this.instructors.addAll(instructors);
    }
    public void archive() { status = Status.ARCHIVED; }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public String getTerm() { return term; }
    public Status getStatus() { return status; }
    public Set<Instructor> getInstructors() { return instructors; }
}
