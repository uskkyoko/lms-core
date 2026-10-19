package com.example.demo.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Role;
import com.example.demo.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

	List<User> findByRoleOrderByFullNameAsc(Role role);
}
