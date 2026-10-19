package com.example.demo.course;

import com.example.demo.user.Student;
import jakarta.persistence.*;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "student_id"}))
public class Enrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "course_id")
    private Course course;
    @ManyToOne(optional = false) @JoinColumn(name = "student_id")
    private Student student;
    protected Enrollment() {}
    public Enrollment(Course course, Student student) { this.course = course; this.student = student; }
}
