package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.domain.Announcement;
import com.example.demo.domain.Assignment;
import com.example.demo.domain.Course;
import com.example.demo.domain.DiscussionPost;
import com.example.demo.domain.Grade;
import com.example.demo.domain.Material;
import com.example.demo.domain.MaterialType;
import com.example.demo.domain.Module;
import com.example.demo.domain.Role;
import com.example.demo.domain.Submission;
import com.example.demo.domain.User;
import com.example.demo.repo.AnnouncementRepository;
import com.example.demo.repo.AssignmentRepository;
import com.example.demo.repo.DiscussionPostRepository;
import com.example.demo.repo.GradeRepository;
import com.example.demo.repo.MaterialRepository;
import com.example.demo.repo.ModuleRepository;
import com.example.demo.repo.SubmissionRepository;
import com.example.demo.storage.FileStorageService;
import com.example.demo.storage.StoredFile;
import com.example.demo.web.ForbiddenException;
import com.example.demo.web.dto.content.AnnouncementDto;
import com.example.demo.web.dto.content.AssignmentDto;
import com.example.demo.web.dto.content.CreateAnnouncementRequest;
import com.example.demo.web.dto.content.CreateAssignmentRequest;
import com.example.demo.web.dto.content.CreateDiscussionRequest;
import com.example.demo.web.dto.content.CreateMaterialRequest;
import com.example.demo.web.dto.content.CreateModuleRequest;
import com.example.demo.web.dto.content.DiscussionDto;
import com.example.demo.web.dto.content.GradeDto;
import com.example.demo.web.dto.content.GradeSubmissionRequest;
import com.example.demo.web.dto.content.MaterialDto;
import com.example.demo.web.dto.content.ModuleDto;
import com.example.demo.web.dto.content.SubmissionDto;

@Service
public class ContentService {

	public static final Set<String> DOCUMENT_EXTENSIONS = Set.of("txt", "pdf", "docx");
	public static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm");
	public static final String DOCUMENT_TYPE_MESSAGE = "This file type is not allowed. Upload a TXT, DOCX, or PDF file.";
	public static final String VIDEO_TYPE_MESSAGE = "This video type is not allowed. Upload an MP4 or WebM file, or use an embed link.";
	public static final String DEADLINE_MESSAGE = "The deadline has passed. This assignment no longer accepts new or replacement submissions.";

	private final LmsService lms;
	private final ModuleRepository modules;
	private final MaterialRepository materials;
	private final AssignmentRepository assignments;
	private final SubmissionRepository submissions;
	private final GradeRepository grades;
	private final AnnouncementRepository announcements;
	private final DiscussionPostRepository discussions;
	private final FileStorageService files;

	public ContentService(
			LmsService lms,
			ModuleRepository modules,
			MaterialRepository materials,
			AssignmentRepository assignments,
			SubmissionRepository submissions,
			GradeRepository grades,
			AnnouncementRepository announcements,
			DiscussionPostRepository discussions,
			FileStorageService files) {
		this.lms = lms;
		this.modules = modules;
		this.materials = materials;
		this.assignments = assignments;
		this.submissions = submissions;
		this.grades = grades;
		this.announcements = announcements;
		this.discussions = discussions;
		this.files = files;
	}

	@Transactional(readOnly = true)
	public List<ModuleDto> listModules(User actor, Long courseId) {
		Course course = visibleCourse(actor, courseId);
		boolean staff = isStaff(actor, course);
		return modules.findByCourseOrderByOrderNumberAsc(course).stream()
				.map(module -> toModuleDto(module, staff))
				.toList();
	}

	@Transactional
	public ModuleDto createModule(User actor, Long courseId, CreateModuleRequest request) {
		Course course = visibleCourse(actor, courseId);
		lms.requireInstructorOrAdmin(actor, course);
		int order = modules.findByCourseOrderByOrderNumberAsc(course).size() + 1;
		String description = request.description() == null ? "" : request.description().trim();
		Module saved = modules.save(new Module(course, request.title().trim(), description, order));
		return toModuleDto(saved, true);
	}

	@Transactional
	public MaterialDto createMaterial(User actor, Long moduleId, CreateMaterialRequest request) {
		Module module = requireModule(moduleId);
		lms.requireInstructorOrAdmin(actor, module.getCourse());
		if (request.type() == MaterialType.FILE) {
			throw new IllegalArgumentException("Upload a TXT, DOCX, or PDF file for file materials.");
		}
		boolean published = Boolean.TRUE.equals(request.published());
		Material material = new Material(module, request.title().trim(), request.type(), published, actor);
		if (request.type() == MaterialType.TEXT) {
			if (request.body() == null || request.body().isBlank()) {
				throw new IllegalArgumentException("Text page content is required.");
			}
			material.setBody(request.body().trim());
		} else if (request.embedUrl() == null || request.embedUrl().isBlank()) {
			throw new IllegalArgumentException("Provide a video embed link or upload a video file.");
		} else {
			material.setEmbedUrl(request.embedUrl().trim());
		}
		return MaterialDto.from(materials.save(material));
	}

	@Transactional
	public MaterialDto uploadMaterial(User actor, Long moduleId, String title, MaterialType type, boolean published, MultipartFile file) {
		Module module = requireModule(moduleId);
		lms.requireInstructorOrAdmin(actor, module.getCourse());
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("Material title is required.");
		}
		if (type == MaterialType.TEXT) {
			throw new IllegalArgumentException("Text pages do not use file upload.");
		}
		StoredFile stored = type == MaterialType.VIDEO
				? files.store(file, VIDEO_EXTENSIONS, VIDEO_TYPE_MESSAGE)
				: files.store(file, DOCUMENT_EXTENSIONS, DOCUMENT_TYPE_MESSAGE);
		MaterialType savedType = type == MaterialType.VIDEO ? MaterialType.VIDEO : MaterialType.FILE;
		Material material = new Material(module, title.trim(), savedType, published, actor);
		material.setStoredFileName(stored.storedFileName());
		material.setOriginalFileName(stored.originalFileName());
		material.setContentType(stored.contentType());
		return MaterialDto.from(materials.save(material));
	}

	@Transactional
	public MaterialDto setPublished(User actor, Long materialId, boolean published) {
		Material material = requireMaterial(materialId);
		lms.requireInstructorOrAdmin(actor, material.getModule().getCourse());
		material.setPublished(published);
		return MaterialDto.from(material);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<Resource> downloadMaterial(User actor, Long materialId) {
		Material material = requireMaterial(materialId);
		Course course = material.getModule().getCourse();
		lms.requireVisibleCourse(actor, course);
		if (!material.isPublished() && !isStaff(actor, course)) {
			throw new ForbiddenException("This material is hidden.");
		}
		if (!material.hasFile()) {
			throw new IllegalArgumentException("This material has no file to download.");
		}
		return fileResponse(material.getStoredFileName(), material.getOriginalFileName(), material.getContentType());
	}

	@Transactional(readOnly = true)
	public List<AssignmentDto> listAssignments(User actor, Long courseId) {
		Course course = visibleCourse(actor, courseId);
		boolean staff = isStaff(actor, course);
		return assignments.findByModuleCourseOrderByDeadlineAscIdAsc(course).stream()
				.filter(assignment -> staff || assignment.isPublished())
				.map(AssignmentDto::from)
				.toList();
	}

	@Transactional
	public AssignmentDto createAssignment(User actor, Long courseId, CreateAssignmentRequest request) {
		Course course = visibleCourse(actor, courseId);
		lms.requireInstructorOrAdmin(actor, course);
		Module module = request.moduleId() == null
				? modules.findByCourseOrderByOrderNumberAsc(course).stream().findFirst()
						.orElseThrow(() -> new IllegalArgumentException("Create a module before adding an assignment."))
				: requireModule(request.moduleId());
		if (!module.getCourse().getId().equals(course.getId())) {
			throw new IllegalArgumentException("The module does not belong to this course.");
		}
		Assignment assignment = new Assignment();
		assignment.setModule(module);
		assignment.setTitle(request.title().trim());
		assignment.setInstructions(request.instructions() == null ? "" : request.instructions().trim());
		assignment.setDeadline(request.deadline());
		assignment.setMaxPoints(request.maxPoints() == null ? 100 : request.maxPoints());
		assignment.setPublished(request.published() == null || request.published());
		return AssignmentDto.from(assignments.save(assignment));
	}

	@Transactional
	public SubmissionDto submitAssignment(User actor, Long assignmentId, MultipartFile file) {
		if (actor.getRole() != Role.STUDENT) {
			throw new ForbiddenException("Only enrolled students can submit assignments.");
		}
		Assignment assignment = requireAssignment(assignmentId);
		Course course = assignment.getModule().getCourse();
		lms.requireEnrolledStudent(actor, course);
		if (!assignment.isPublished()) {
			throw new ForbiddenException("This assignment is not available.");
		}
		if (!assignment.isOpen()) {
			throw new IllegalArgumentException(DEADLINE_MESSAGE);
		}
		StoredFile stored = files.store(file, DOCUMENT_EXTENSIONS, DOCUMENT_TYPE_MESSAGE);
		LocalDateTime now = LocalDateTime.now();
		Submission existing = submissions.findByAssignmentAndStudent(assignment, actor).orElse(null);
		if (existing == null) {
			Submission created = submissions.save(new Submission(
					assignment, actor, stored.storedFileName(), stored.originalFileName(), stored.contentType(), now));
			return toSubmissionDto(created);
		}
		files.deleteQuietly(existing.getStoredFileName());
		existing.setStoredFileName(stored.storedFileName());
		existing.setOriginalFileName(stored.originalFileName());
		existing.setContentType(stored.contentType());
		existing.setSubmittedAt(now);
		return toSubmissionDto(existing);
	}

	@Transactional(readOnly = true)
	public List<SubmissionDto> listSubmissions(User actor, Long assignmentId) {
		Assignment assignment = requireAssignment(assignmentId);
		Course course = assignment.getModule().getCourse();
		lms.requireVisibleCourse(actor, course);
		if (isStaff(actor, course)) {
			return submissions.findByAssignmentOrderBySubmittedAtAsc(assignment).stream().map(this::toSubmissionDto).toList();
		}
		if (actor.getRole() != Role.STUDENT) {
			throw new ForbiddenException("You cannot view submissions for this assignment.");
		}
		return submissions.findByAssignmentAndStudent(assignment, actor).stream().map(this::toSubmissionDto).toList();
	}

	@Transactional(readOnly = true)
	public ResponseEntity<Resource> downloadSubmission(User actor, Long submissionId) {
		Submission submission = requireSubmission(submissionId);
		Course course = submission.getAssignment().getModule().getCourse();
		lms.requireVisibleCourse(actor, course);
		boolean own = submission.getStudent().getId().equals(actor.getId());
		if (!own && !isStaff(actor, course)) {
			throw new ForbiddenException("You can only download your own submission.");
		}
		return fileResponse(submission.getStoredFileName(), submission.getOriginalFileName(), submission.getContentType());
	}

	@Transactional
	public SubmissionDto gradeSubmission(User actor, Long submissionId, GradeSubmissionRequest request) {
		Submission submission = requireSubmission(submissionId);
		Assignment assignment = submission.getAssignment();
		Course course = assignment.getModule().getCourse();
		lms.requireInstructorOrAdmin(actor, course);
		if (request.points() < 0 || request.points() > assignment.getMaxPoints()) {
			throw new IllegalArgumentException("Score must be between 0 and " + assignment.getMaxPoints() + ".");
		}
		Grade grade = grades.findByStudentAndAssignment(submission.getStudent(), assignment).orElseGet(Grade::new);
		grade.setStudent(submission.getStudent());
		grade.setAssignment(assignment);
		grade.setPoints(request.points());
		grade.setMaxPoints(assignment.getMaxPoints());
		grade.setFeedback(request.feedback() == null ? "" : request.feedback().trim());
		grade.setGradedAt(LocalDateTime.now());
		grades.save(grade);
		return toSubmissionDto(submission);
	}

	@Transactional(readOnly = true)
	public List<AnnouncementDto> listAnnouncements(User actor, Long courseId) {
		Course course = visibleCourse(actor, courseId);
		return announcements.findByCourseOrderByCreatedAtDesc(course).stream().map(AnnouncementDto::from).toList();
	}

	@Transactional
	public AnnouncementDto createAnnouncement(User actor, Long courseId, CreateAnnouncementRequest request) {
		Course course = visibleCourse(actor, courseId);
		lms.requireInstructorOrAdmin(actor, course);
		Announcement saved = announcements.save(new Announcement(
				course, request.title().trim(), request.body().trim(), actor, LocalDateTime.now()));
		return AnnouncementDto.from(saved);
	}

	@Transactional(readOnly = true)
	public List<DiscussionDto> listDiscussions(User actor, Long courseId) {
		Course course = visibleCourse(actor, courseId);
		return discussions.findByCourseAndParentIsNullOrderByCreatedAtDesc(course).stream().map(this::toDiscussionDto).toList();
	}

	@Transactional
	public DiscussionDto createDiscussion(User actor, Long courseId, CreateDiscussionRequest request) {
		Course course = visibleCourse(actor, courseId);
		if (request.title() == null || request.title().isBlank()) {
			throw new IllegalArgumentException("Discussion title is required.");
		}
		DiscussionPost saved = discussions.save(new DiscussionPost(
				course, null, request.title().trim(), request.body().trim(), actor, LocalDateTime.now()));
		return toDiscussionDto(saved);
	}

	@Transactional
	public DiscussionDto reply(User actor, Long postId, CreateDiscussionRequest request) {
		DiscussionPost parent = requireDiscussion(postId);
		if (parent.isReply()) {
			throw new IllegalArgumentException("Replies can only be added to a top-level post.");
		}
		lms.requireVisibleCourse(actor, parent.getCourse());
		DiscussionPost saved = discussions.save(new DiscussionPost(
				parent.getCourse(), parent, null, request.body().trim(), actor, LocalDateTime.now()));
		parent.getReplies().add(saved);
		return toDiscussionDto(saved);
	}

	@Transactional
	public DiscussionDto editDiscussion(User actor, Long postId, CreateDiscussionRequest request) {
		DiscussionPost post = requireDiscussion(postId);
		lms.requireVisibleCourse(actor, post.getCourse());
		if (!post.getAuthor().getId().equals(actor.getId())) {
			throw new ForbiddenException("You can only edit your own posts.");
		}
		if (!post.isReply() && request.title() != null && !request.title().isBlank()) {
			post.setTitle(request.title().trim());
		}
		post.setBody(request.body().trim());
		post.setUpdatedAt(LocalDateTime.now());
		return toDiscussionDto(post);
	}

	@Transactional
	public void deleteDiscussion(User actor, Long postId) {
		DiscussionPost post = requireDiscussion(postId);
		Course course = post.getCourse();
		lms.requireVisibleCourse(actor, course);
		boolean author = post.getAuthor().getId().equals(actor.getId());
		if (!author && !isStaff(actor, course)) {
			throw new ForbiddenException("You can only delete your own posts unless you are the course instructor or an administrator.");
		}
		discussions.delete(post);
	}

	private ModuleDto toModuleDto(Module module, boolean staff) {
		List<MaterialDto> visible = materials.findByModuleOrderByIdAsc(module).stream()
				.filter(material -> staff || material.isPublished())
				.map(MaterialDto::from)
				.toList();
		return ModuleDto.from(module, visible);
	}

	private SubmissionDto toSubmissionDto(Submission submission) {
		GradeDto grade = grades.findByStudentAndAssignment(submission.getStudent(), submission.getAssignment())
				.map(GradeDto::from)
				.orElse(null);
		return SubmissionDto.from(submission, grade);
	}

	private DiscussionDto toDiscussionDto(DiscussionPost post) {
		List<DiscussionDto> replies = post.getReplies().stream()
				.map(reply -> DiscussionDto.from(reply, List.of()))
				.toList();
		return DiscussionDto.from(post, replies);
	}

	private ResponseEntity<Resource> fileResponse(String storedName, String originalName, String contentType) {
		Resource resource = files.load(storedName);
		String type = contentType == null || contentType.isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType;
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalName.replace("\"", "") + "\"")
				.contentType(MediaType.parseMediaType(type))
				.body(resource);
	}

	private Course visibleCourse(User actor, Long courseId) {
		Course course = lms.requireCourse(courseId);
		lms.requireVisibleCourse(actor, course);
		return course;
	}

	private boolean isStaff(User actor, Course course) {
		if (actor.getRole() == Role.ADMINISTRATOR) {
			return true;
		}
		return actor.getRole() == Role.INSTRUCTOR
				&& course.getInstructors().stream().anyMatch(instructor -> instructor.getId().equals(actor.getId()));
	}

	private Module requireModule(Long moduleId) {
		return modules.findById(moduleId).orElseThrow(() -> new IllegalArgumentException("Module not found."));
	}

	private Material requireMaterial(Long materialId) {
		return materials.findById(materialId).orElseThrow(() -> new IllegalArgumentException("Material not found."));
	}

	private Assignment requireAssignment(Long assignmentId) {
		return assignments.findById(assignmentId).orElseThrow(() -> new IllegalArgumentException("Assignment not found."));
	}

	private Submission requireSubmission(Long submissionId) {
		return submissions.findById(submissionId).orElseThrow(() -> new IllegalArgumentException("Submission not found."));
	}

	private DiscussionPost requireDiscussion(Long postId) {
		return discussions.findById(postId).orElseThrow(() -> new IllegalArgumentException("Discussion post not found."));
	}
}
