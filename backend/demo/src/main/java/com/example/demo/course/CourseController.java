package com.example.demo.course;

import com.example.demo.user.ActiveUserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courses;
    private final ActiveUserService active;
    public CourseController(CourseService courses, ActiveUserService active) { this.courses = courses; this.active = active; }
    @GetMapping
    public List<CourseService.CourseView> list(HttpSession session) { return courses.list(active.current(session)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CourseService.CourseView create(@Valid @RequestBody CourseRequest request, HttpSession session) {
        active.requireAdministrator(session); return courses.save(null, request);
    }
    @PutMapping("/{id}")
    public CourseService.CourseView update(@PathVariable long id, @Valid @RequestBody CourseRequest request, HttpSession session) {
        active.requireAdministrator(session); return courses.save(id, request);
    }
    @PatchMapping("/{id}/archive")
    public CourseService.CourseView archive(@PathVariable long id, HttpSession session) {
        active.requireAdministrator(session); return courses.archive(id);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, HttpSession session) {
        active.requireAdministrator(session); courses.delete(id);
    }
}
