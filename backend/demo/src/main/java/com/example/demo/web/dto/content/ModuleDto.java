package com.example.demo.web.dto.content;

import java.util.List;

import com.example.demo.domain.Module;

public record ModuleDto(Long id, Long courseId, String title, String description, int orderNumber, List<MaterialDto> materials) {

	public static ModuleDto from(Module module, List<MaterialDto> materials) {
		return new ModuleDto(module.getId(), module.getCourse().getId(), module.getTitle(), module.getDescription(), module.getOrderNumber(), materials);
	}
}
