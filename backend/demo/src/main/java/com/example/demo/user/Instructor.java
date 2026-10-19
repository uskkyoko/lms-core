package com.example.demo.user;
import jakarta.persistence.Entity;

@Entity
public class Instructor extends User {
    protected Instructor() {}
    public Instructor(String name, String email) { super(name, email); }
    @Override public Role getRole() { return Role.INSTRUCTOR; }
}
