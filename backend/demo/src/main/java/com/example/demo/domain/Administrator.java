package com.example.demo.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity @DiscriminatorValue("ADMINISTRATOR")
public class Administrator extends User {
    protected Administrator() {}
    public Administrator(String fullName, String email) { super(fullName, email, Role.ADMINISTRATOR); }
}
