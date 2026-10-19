package com.example.demo.web.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollRequest(@NotNull(message = "Student is required.") Long studentId) {
}
