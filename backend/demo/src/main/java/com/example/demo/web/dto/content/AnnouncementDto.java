package com.example.demo.web.dto.content;

import java.time.LocalDateTime;

import com.example.demo.domain.Announcement;
import com.example.demo.web.dto.UserDto;

public record AnnouncementDto(Long id, Long courseId, String title, String body, UserDto author, LocalDateTime createdAt) {

	public static AnnouncementDto from(Announcement announcement) {
		return new AnnouncementDto(
				announcement.getId(),
				announcement.getCourse().getId(),
				announcement.getTitle(),
				announcement.getBody(),
				UserDto.from(announcement.getAuthor()),
				announcement.getCreatedAt());
	}
}
