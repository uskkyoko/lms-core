package com.example.demo.user;

import jakarta.persistence.*;

@Entity
@Table(name = "lms_users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type")
public abstract class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String email;

    protected User() {}
    protected User(String name, String email) { this.name = name; this.email = email; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public void updateIdentity(String name, String email) { this.name = name; this.email = email; }
    public abstract Role getRole();
}
