package com.example.demo.web.dto.content;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.domain.DiscussionPost;
import com.example.demo.web.dto.UserDto;

public record DiscussionDto(
		Long id,
		Long courseId,
		Long parentId,
		String title,
		String body,
		UserDto author,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		List<DiscussionDto> replies) {

	public static DiscussionDto from(DiscussionPost post, List<DiscussionDto> replies) {
		return new DiscussionDto(
				post.getId(),
				post.getCourse().getId(),
				post.getParent() == null ? null : post.getParent().getId(),
				post.getTitle(),
				post.getBody(),
				UserDto.from(post.getAuthor()),
				post.getCreatedAt(),
				post.getUpdatedAt(),
				replies);
	}
}
