package com.example.demo.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity @DiscriminatorValue("INSTRUCTOR")
public class Instructor extends User {
    protected Instructor() {}
    public Instructor(String fullName, String email) { super(fullName, email, Role.INSTRUCTOR); }
}
