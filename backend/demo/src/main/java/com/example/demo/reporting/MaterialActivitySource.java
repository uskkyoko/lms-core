package com.example.demo.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Course;
import com.example.demo.domain.Material;
import com.example.demo.domain.MaterialCompletion;
import com.example.demo.repo.MaterialCompletionRepository;
import com.example.demo.repo.MaterialRepository;
import com.example.demo.service.LmsService;

@Component
@Transactional(readOnly = true)
public class MaterialActivitySource implements CourseActivitySource {

	private final MaterialRepository materials;
	private final MaterialCompletionRepository completions;
	private final LmsService lms;

	public MaterialActivitySource(MaterialRepository materials, MaterialCompletionRepository completions, LmsService lms) {
		this.materials = materials;
		this.completions = completions;
		this.lms = lms;
	}

	@Override
	public Set<Kind> supportedKinds() {
		return Set.of(Kind.MATERIAL);
	}

	@Override
	public List<Activity> load(Long courseId, Set<Long> studentIds) {
		Course course = lms.requireCourse(courseId);
		List<MaterialCompletion> done = completions.findByMaterialModuleCourse(course);
		List<Activity> result = new ArrayList<>();
		for (Material material : materials.findByModuleCourseAndPublishedTrueOrderByIdAsc(course)) {
			List<Result> results = new ArrayList<>();
			for (MaterialCompletion completion : done) {
				if (completion.getMaterial().getId().equals(material.getId())
						&& studentIds.contains(completion.getStudent().getId())) {
					results.add(new Result(completion.getStudent().getId(), true, null));
				}
			}
			result.add(new Activity(Kind.MATERIAL, material.getId(), material.getTitle(), null, results));
		}
		return result;
	}
}
