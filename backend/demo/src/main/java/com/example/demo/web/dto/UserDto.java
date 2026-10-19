package com.example.demo.web.dto;

import com.example.demo.domain.Role;
import com.example.demo.domain.User;

public record UserDto(Long id, String fullName, String email, Role role) {

	public static UserDto from(User user) {
		return new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
	}
}
