package com.example.demo.web.dto.content;

import com.example.demo.domain.Material;
import com.example.demo.domain.MaterialType;
import com.example.demo.web.dto.UserDto;

public record MaterialDto(
		Long id,
		Long moduleId,
		String title,
		MaterialType type,
		String body,
		String embedUrl,
		String originalFileName,
		boolean hasFile,
		boolean published,
		UserDto createdBy) {

	public static MaterialDto from(Material material) {
		return new MaterialDto(
				material.getId(),
				material.getModule().getId(),
				material.getTitle(),
				material.getType(),
				material.getBody(),
				material.getEmbedUrl(),
				material.getOriginalFileName(),
				material.hasFile(),
				material.isPublished(),
				UserDto.from(material.getCreatedBy()));
	}
}
