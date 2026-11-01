package com.example.demo.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Announcement;
import com.example.demo.domain.Course;
import com.example.demo.domain.DiscussionPost;
import com.example.demo.domain.Material;
import com.example.demo.domain.MaterialType;
import com.example.demo.domain.Module;
import com.example.demo.domain.User;
import com.example.demo.repo.AnnouncementRepository;
import com.example.demo.repo.CourseRepository;
import com.example.demo.repo.DiscussionPostRepository;
import com.example.demo.repo.MaterialRepository;
import com.example.demo.repo.ModuleRepository;
import com.example.demo.repo.UserRepository;

@Component
@Order(2)
public class ContentDemoDataLoader implements CommandLineRunner {

	private final CourseRepository courses;
	private final UserRepository users;
	private final ModuleRepository modules;
	private final MaterialRepository materials;
	private final AnnouncementRepository announcements;
	private final DiscussionPostRepository discussions;

	public ContentDemoDataLoader(
			CourseRepository courses,
			UserRepository users,
			ModuleRepository modules,
			MaterialRepository materials,
			AnnouncementRepository announcements,
			DiscussionPostRepository discussions) {
		this.courses = courses;
		this.users = users;
		this.modules = modules;
		this.materials = materials;
		this.announcements = announcements;
		this.discussions = discussions;
	}

	@Override
	@Transactional
	public void run(String... args) {
		Course se331 = courses.findByCodeIgnoreCase("SE331").orElse(null);
		if (se331 == null) {
			return;
		}
		User instructor = users.findAll().stream()
				.filter(user -> "andrii.shevchenko@auk.edu.ua".equals(user.getEmail()))
				.findFirst()
				.orElseThrow();
		boolean hasSyllabus = materials.findByModuleCourseAndPublishedTrueOrderByIdAsc(se331).stream()
				.anyMatch(item -> "Syllabus".equals(item.getTitle()));
		if (!hasSyllabus) {
			Module week1 = modules.findByCourseOrderByOrderNumberAsc(se331).get(0);
			Material syllabus = new Material(week1, "Syllabus", MaterialType.TEXT, true, instructor);
			syllabus.setBody("Welcome to SE331. Read the weekly materials, submit assignments before the deadline, and use Discussions for questions.");
			materials.save(syllabus);
			Material hidden = new Material(week1, "Instructor notes", MaterialType.TEXT, false, instructor);
			hidden.setBody("Hidden from students until published.");
			materials.save(hidden);
			Material video = new Material(week1, "Course overview", MaterialType.VIDEO, true, instructor);
			video.setEmbedUrl("https://www.youtube.com/embed/8mAITcNt710");
			materials.save(video);
			announcements.save(new Announcement(
					se331,
					"Welcome to SE331",
					"The first lab is open. Submit before the deadline; replacements are allowed until then.",
					instructor,
					LocalDateTime.now()));
		}
		if (discussions.findByCourseAndParentIsNullOrderByCreatedAtDesc(se331).isEmpty()) {
			discussions.save(new DiscussionPost(
					se331,
					null,
					"Week 1 questions",
					"Use this thread for questions about this week's materials and the first assignment. Students can reply here.",
					instructor,
					LocalDateTime.now()));
		}
	}
}
