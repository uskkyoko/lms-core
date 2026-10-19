package com.example.demo.user;
import jakarta.persistence.Entity;

@Entity
public class Student extends User {
    protected Student() {}
    public Student(String name, String email) { super(name, email); }
    @Override public Role getRole() { return Role.STUDENT; }
}
