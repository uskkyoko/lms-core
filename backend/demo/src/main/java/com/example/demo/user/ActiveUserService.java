package com.example.demo.user;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActiveUserService {
    private final UserRepository users;
    public ActiveUserService(UserRepository users) { this.users = users; }

    public User current(HttpSession session) {
        Long id = (Long) session.getAttribute("activeUserId");
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Select a sample user first.");
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Select a sample user first."));
    }

    public User select(long id, HttpSession session) {
        User user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sample user not found."));
        session.setAttribute("activeUserId", user.getId());
        return user;
    }

    public void requireAdministrator(HttpSession session) {
        if (current(session).getRole() != Role.ADMINISTRATOR)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can manage courses.");
    }
}
