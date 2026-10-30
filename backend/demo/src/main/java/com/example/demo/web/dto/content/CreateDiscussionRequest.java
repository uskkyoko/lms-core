package com.example.demo.web.dto.content;

import jakarta.validation.constraints.NotBlank;

public record CreateDiscussionRequest(String title, @NotBlank(message = "Discussion text is required.") String body) {
}
