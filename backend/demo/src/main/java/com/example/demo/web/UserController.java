package com.example.demo.web;

import java.util.List;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.domain.Role;
import com.example.demo.service.LmsService;
import com.example.demo.web.dto.UserDto;

@RestController
@RequestMapping("/api")

public class UserController {

	private final LmsService lmsService;

	public UserController(LmsService lmsService) {
		this.lmsService = lmsService;
	}

	@GetMapping("/users")
	public List<UserDto> users(@RequestParam(required = false) Role role) {
		return lmsService.listUsers(role);
	}

	@GetMapping("/me")
	public UserDto me(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
		return UserDto.from(lmsService.requireUser(userId));
	}
}
