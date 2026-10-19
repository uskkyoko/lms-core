package com.example.demo.course;

import com.example.demo.user.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CourseService {
    private final CourseRepository courses;
    private final UserRepository users;
    private final EnrollmentRepository enrollments;
    public CourseService(CourseRepository courses, UserRepository users, EnrollmentRepository enrollments) {
        this.courses = courses; this.users = users; this.enrollments = enrollments;
    }
    @Transactional(readOnly = true)
    public List<CourseView> list(User user) {
        List<Course> result = switch (user.getRole()) {
            case ADMINISTRATOR -> courses.findAll(org.springframework.data.domain.Sort.by("id"));
            case INSTRUCTOR -> courses.findAssigned(user.getId());
            case STUDENT -> courses.findEnrolled(user.getId());
        };
        return result.stream().map(CourseView::from).toList();
    }
    public CourseView save(Long id, CourseRequest request) {
        Course existing = id == null ? null : get(id);
        String code = request.code().strip().toUpperCase(Locale.ROOT);
        boolean duplicate = id == null ? courses.existsByCode(code) : courses.existsByCodeAndIdNot(code, id);
        if (duplicate) throw new ResponseStatusException(HttpStatus.CONFLICT, "Course code already exists. Choose a unique code.");
        Set<Instructor> instructors = new LinkedHashSet<>();
        for (Long instructorId : request.instructorIds()) {
            User user = users.findById(instructorId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select an existing instructor."));
            if (!(user instanceof Instructor instructor)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned users must be instructors.");
            instructors.add(instructor);
        }
        if (existing == null) existing = new Course(request.title().strip(), code, request.description().strip(), request.term().strip(), instructors);
        else existing.update(request.title().strip(), code, request.description().strip(), request.term().strip(), instructors);
        return CourseView.from(courses.saveAndFlush(existing));
    }
    public CourseView archive(long id) {
        Course course = get(id); course.archive(); return CourseView.from(course);
    }
    public void delete(long id) {
        Course course = get(id);
        enrollments.deleteByCourseId(id);
        courses.delete(course);
    }
    private Course get(long id) {
        return courses.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));
    }
    public record InstructorView(Long id, String name) {}
    public record CourseView(Long id, String title, String code, String description, String term, Course.Status status, List<InstructorView> instructors) {
        public static CourseView from(Course course) {
            return new CourseView(course.getId(), course.getTitle(), course.getCode(), course.getDescription(), course.getTerm(), course.getStatus(),
                course.getInstructors().stream().sorted(Comparator.comparing(User::getId)).map(i -> new InstructorView(i.getId(), i.getName())).toList());
        }
    }
}
