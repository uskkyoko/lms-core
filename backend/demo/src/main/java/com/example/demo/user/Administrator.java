package com.example.demo.user;
import jakarta.persistence.Entity;

@Entity
public class Administrator extends User {
    protected Administrator() {}
    public Administrator(String name, String email) { super(name, email); }
    @Override public Role getRole() { return Role.ADMINISTRATOR; }
}
