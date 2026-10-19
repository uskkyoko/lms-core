package com.example.demo.user;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserRepository users;
    private final ActiveUserService active;
    public UserController(UserRepository users, ActiveUserService active) { this.users = users; this.active = active; }

    @GetMapping("/users")
    public List<User> users(@RequestParam(required = false) Role role) {
        return users.findAll().stream().filter(user -> role == null || user.getRole() == role).toList();
    }
    @GetMapping("/session")
    public User current(HttpSession session) { return active.current(session); }
    @PutMapping("/session")
    public User select(@Valid @RequestBody Selection selection, HttpSession session) {
        return active.select(selection.userId(), session);
    }
    public record Selection(@NotNull @Positive Long userId) {}
}
