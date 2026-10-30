package com.example.demo.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Material;
import com.example.demo.repo.MaterialRepository;
import com.example.demo.service.LmsService;

@Component
@Transactional(readOnly = true)
public class MaterialActivitySource implements CourseActivitySource {

	private final MaterialRepository materials;
	private final LmsService lms;

	public MaterialActivitySource(MaterialRepository materials, LmsService lms) {
		this.materials = materials;
		this.lms = lms;
	}

	@Override
	public Set<Kind> supportedKinds() {
		return Set.of(Kind.MATERIAL);
	}

	@Override
	public List<Activity> load(Long courseId, Set<Long> studentIds) {
		List<Activity> result = new ArrayList<>();
		for (Material material : materials.findByModuleCourseAndPublishedTrueOrderByIdAsc(lms.requireCourse(courseId))) {
			result.add(new Activity(Kind.MATERIAL, material.getId(), material.getTitle(), null, List.of()));
		}
		return result;
	}
}
