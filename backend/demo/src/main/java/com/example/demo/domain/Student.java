package com.example.demo.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity @DiscriminatorValue("STUDENT")
public class Student extends User {
    protected Student() {}
    public Student(String fullName, String email) { super(fullName, email, Role.STUDENT); }
}
