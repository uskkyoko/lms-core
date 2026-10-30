package com.example.demo.service;

import java.util.*;
import com.example.demo.domain.*;
import com.example.demo.repo.*;
import com.example.demo.web.ForbiddenException;
import com.example.demo.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Shared role and course-access boundary for all LMS modules. */
@Service
@Transactional(readOnly = true)
public class LmsService {
    private final UserRepository users;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final ModuleRepository modules;
    private final QuizRepository quizzes;
    private final AssignmentRepository assignments;
    private final MaterialRepository materials;
    private final AnnouncementRepository announcements;
    private final DiscussionPostRepository discussions;

    public LmsService(UserRepository users, CourseRepository courses, EnrollmentRepository enrollments,
                      ModuleRepository modules, QuizRepository quizzes, AssignmentRepository assignments,
                      MaterialRepository materials, AnnouncementRepository announcements, DiscussionPostRepository discussions) {
        this.users = users; this.courses = courses; this.enrollments = enrollments;
        this.modules = modules; this.quizzes = quizzes; this.assignments = assignments;
        this.materials = materials; this.announcements = announcements; this.discussions = discussions;
    }
    public User requireUser(Long userId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Select a sample user first.");
        return users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Select an existing sample user."));
    }
    public List<UserDto> listUsers(Role role) {
        return (role == null ? users.findAll() : users.findByRoleOrderByFullNameAsc(role)).stream().map(UserDto::from).toList();
    }
    public List<CourseDto> listCourses(User actor) {
        List<Course> visible = switch (actor.getRole()) {
            case ADMINISTRATOR -> courses.findAllByOrderByCodeAsc();
            case INSTRUCTOR -> courses.findByInstructorsContainingOrderByCodeAsc(actor);
            case STUDENT -> enrollments.findByStudentOrderByCourseCodeAsc(actor).stream().map(Enrollment::getCourse).toList();
        };
        return visible.stream().map(c -> toDto(c, actor)).toList();
    }
    @Transactional
    public CourseDto createCourse(User actor, CreateCourseRequest request) { return saveCourse(actor, null, request); }
    @Transactional
    public CourseDto updateCourse(User actor, Long id, CreateCourseRequest request) { return saveCourse(actor, id, request); }
    private CourseDto saveCourse(User actor, Long id, CreateCourseRequest request) {
        requireAdmin(actor);
        Course course = id == null ? new Course(request.title().strip(), "", request.description().strip(), request.term().strip()) : requireCourse(id);
        String code = request.code().strip().toUpperCase(Locale.ROOT);
        if (id == null ? courses.existsByCodeIgnoreCase(code) : courses.existsByCodeIgnoreCaseAndIdNot(code, id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course code already exists. Choose a different unique course code.");
        Set<User> instructors = resolveInstructors(request.instructorIds());
        course.setTitle(request.title().strip()); course.setCode(code);
        course.setDescription(request.description().strip()); course.setTerm(request.term().strip());
        course.getInstructors().clear(); course.getInstructors().addAll(instructors);
        return toDto(courses.saveAndFlush(course), actor);
    }
    @Transactional
    public CourseDto archiveCourse(User actor, Long id) {
        requireAdmin(actor); Course course = requireCourse(id); course.setStatus(CourseStatus.ARCHIVED);
        return toDto(course, actor);
    }
    @Transactional
    public void deleteCourse(User actor, Long id) {
        requireAdmin(actor); Course course = requireCourse(id);
        // Retain learning records and immutable grade history. Archive these courses instead.
        if (quizzes.existsByModuleCourse(course) || assignments.existsByModuleCourse(course))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This course contains assessments. Archive it to preserve learning records and grade history.");
        materials.deleteByModuleCourse(course);
        discussions.deleteByCourseAndParentIsNotNull(course);
        discussions.deleteByCourse(course);
        announcements.deleteByCourse(course);
        enrollments.deleteByCourse(course);
        modules.deleteByCourse(course);
        courses.delete(course);
        courses.flush();
    }
    @Transactional
    public CourseDto assignInstructors(User actor, Long id, Set<Long> ids) {
        requireAdmin(actor); Course course = requireCourse(id);
        Set<User> instructors = resolveInstructors(ids);
        course.getInstructors().clear(); course.getInstructors().addAll(instructors);
        return toDto(course, actor);
    }
    private Set<User> resolveInstructors(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("Assign at least one instructor.");
        Set<User> result = new LinkedHashSet<>();
        for (Long id : ids) {
            User instructor = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Instructor not found."));
            if (instructor.getRole() != Role.INSTRUCTOR) throw new IllegalArgumentException("Assigned users must be instructors.");
            result.add(instructor);
        }
        return result;
    }
    @Transactional
    public CourseDto enrollStudent(User actor, Long courseId, Long studentId) {
        requireAdmin(actor); Course course = requireCourse(courseId);
        User student = users.findById(studentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found."));
        if (student.getRole() != Role.STUDENT) throw new IllegalArgumentException("Only students can be enrolled in a course.");
        Enrollment existing = enrollments.findByCourseAndStudent(course, student).orElse(null);
        if (existing != null && existing.getStatus() == Enrollment.Status.ACTIVE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This student is already enrolled in the course.");
        if (existing == null) enrollments.saveAndFlush(new Enrollment(course, student));
        else existing.reactivate();
        return toDto(course, actor);
    }
    @Transactional
    public CourseDto removeStudent(User actor, Long courseId, Long studentId) {
        requireAdmin(actor); Course course = requireCourse(courseId);
        User student = users.findById(studentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found."));
        Enrollment enrollment = enrollments.findByCourseAndStudent(course, student)
            .filter(e -> e.getStatus() == Enrollment.Status.ACTIVE)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active enrollment not found."));
        enrollment.remove();
        return toDto(course, actor);
    }
    public Course requireCourse(Long courseId) {
        return courses.findById(courseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));
    }
    public void requireVisibleCourse(User actor, Course course) {
        boolean allowed = switch (actor.getRole()) {
            case ADMINISTRATOR -> true;
            case INSTRUCTOR -> isAssignedInstructor(course, actor);
            case STUDENT -> enrollments.existsByCourseAndStudent(course, actor);
        };
        if (!allowed) throw new ForbiddenException("You do not have access to this course.");
    }
    public void requireInstructorOrAdmin(User actor, Course course) {
        if (actor.getRole() != Role.ADMINISTRATOR) requireAssignedInstructor(actor, course);
    }
    public void requireAssignedInstructor(User actor, Course course) {
        if (actor.getRole() != Role.INSTRUCTOR || !isAssignedInstructor(course, actor))
            throw new ForbiddenException("Only assigned instructors can access this course for this role.");
    }
    public void requireEnrolledStudent(User actor, Course course) {
        if (actor.getRole() != Role.STUDENT || !enrollments.existsByCourseAndStudent(course, actor))
            throw new ForbiddenException("Only enrolled students can access this course for this role.");
    }
    public List<User> enrolledStudents(Course course) {
        return enrollments.findByCourseOrderByStudentFullNameAsc(course).stream().map(Enrollment::getStudent).toList();
    }
    private boolean isAssignedInstructor(Course course, User actor) {
        return course.getInstructors().stream().anyMatch(user -> user.getId().equals(actor.getId()));
    }
    private CourseDto toDto(Course course, User actor) {
        List<UserDto> students = enrolledStudents(course).stream()
            .filter(student -> actor.getRole() != Role.STUDENT || student.getId().equals(actor.getId()))
            .map(UserDto::from).toList();
        return CourseDto.from(course, students);
    }
    public void requireAdmin(User actor) {
        if (actor.getRole() != Role.ADMINISTRATOR) throw new ForbiddenException("Only administrators can manage courses.");
    }
}