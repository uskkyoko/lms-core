package com.example.demo;

import com.example.demo.course.*;
import com.example.demo.user.*;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SampleData implements CommandLineRunner {
    private final UserRepository users;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    public SampleData(UserRepository users, CourseRepository courses, EnrollmentRepository enrollments) {
        this.users = users; this.courses = courses; this.enrollments = enrollments;
    }
    @Override @Transactional
    public void run(String... args) {
        if (users.count() > 0) { updatePreviousSampleUsers(); return; }
        users.save(new Administrator("Uliana", "uliana@example.test"));
        Instructor instructor = users.save(new Instructor("Liza", "liza@example.test"));
        Student first = users.save(new Student("Kseniia", "kseniia@example.test"));
        Course software = courses.save(new Course("Introduction to Software Engineering", "SDT213", "Explore requirements, software design, implementation and testing through a collaborative team project.", "Fall 2026", Set.of(instructor)));
        courses.save(new Course("Database Fundamentals", "SDT205", "Build a foundation in relational data modelling, SQL and database design.", "Fall 2026", Set.of(instructor)));
        enrollments.save(new Enrollment(software, first));
    }

    // Upgrade the original demo identities without resetting any saved courses.
    private void updatePreviousSampleUsers() {
        var existing = users.findAll();
        for (User user : existing) {
            switch (user.getEmail()) {
                case "alex.admin@example.test" -> user.updateIdentity("Uliana", "uliana@example.test");
                case "taylor@example.test" -> user.updateIdentity("Liza", "liza@example.test");
                case "sam@example.test" -> user.updateIdentity("Kseniia", "kseniia@example.test");
                default -> { }
            }
        }
        Instructor liza = existing.stream().filter(user -> user.getEmail().equals("liza@example.test"))
            .filter(Instructor.class::isInstance).map(Instructor.class::cast).findFirst().orElse(null);
        for (User user : existing) {
            if (user.getEmail().equals("jordan@example.test") && liza != null) {
                for (Course course : courses.findAssigned(user.getId())) {
                    course.getInstructors().removeIf(instructor -> instructor.getId().equals(user.getId()));
                    course.getInstructors().add(liza);
                }
                courses.flush();
                users.delete(user);
            } else if (user.getEmail().equals("jamie@example.test")) {
                enrollments.deleteByStudentId(user.getId());
                enrollments.flush();
                users.delete(user);
            }
        }
    }
}
