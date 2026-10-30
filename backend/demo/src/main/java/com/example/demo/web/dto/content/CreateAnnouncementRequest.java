package com.example.demo.web.dto.content;

import jakarta.validation.constraints.NotBlank;

public record CreateAnnouncementRequest(
		@NotBlank(message = "Announcement title is required.") String title,
		@NotBlank(message = "Announcement message is required.") String body) {
}
