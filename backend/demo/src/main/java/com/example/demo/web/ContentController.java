package com.example.demo.web;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.domain.MaterialType;
import com.example.demo.domain.User;
import com.example.demo.service.ContentService;
import com.example.demo.service.LmsService;
import com.example.demo.web.dto.content.AnnouncementDto;
import com.example.demo.web.dto.content.AssignmentDto;
import com.example.demo.web.dto.content.CreateAnnouncementRequest;
import com.example.demo.web.dto.content.CreateAssignmentRequest;
import com.example.demo.web.dto.content.CreateDiscussionRequest;
import com.example.demo.web.dto.content.CreateMaterialRequest;
import com.example.demo.web.dto.content.CreateModuleRequest;
import com.example.demo.web.dto.content.DiscussionDto;
import com.example.demo.web.dto.content.GradeSubmissionRequest;
import com.example.demo.web.dto.content.MaterialDto;
import com.example.demo.web.dto.content.ModuleDto;
import com.example.demo.web.dto.content.PublishRequest;
import com.example.demo.web.dto.content.SubmissionDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ContentController {

	private final LmsService lmsService;
	private final ContentService contentService;

	public ContentController(LmsService lmsService, ContentService contentService) {
		this.lmsService = lmsService;
		this.contentService = contentService;
	}

	@GetMapping("/courses/{courseId}/modules")
	public List<ModuleDto> modules(@RequestHeader(value = "X-User-Id", required = false) Long userId, @PathVariable Long courseId) {
		return contentService.listModules(user(userId), courseId);
	}

	@PostMapping("/courses/{courseId}/modules")
	public ModuleDto createModule(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long courseId,
			@Valid @RequestBody CreateModuleRequest request) {
		return contentService.createModule(user(userId), courseId, request);
	}

	@PostMapping("/modules/{moduleId}/materials")
	public MaterialDto createMaterial(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long moduleId,
			@Valid @RequestBody CreateMaterialRequest request) {
		return contentService.createMaterial(user(userId), moduleId, request);
	}

	@PostMapping("/modules/{moduleId}/materials/upload")
	public MaterialDto uploadMaterial(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long moduleId,
			@RequestParam String title,
			@RequestParam(defaultValue = "FILE") MaterialType type,
			@RequestParam(defaultValue = "false") boolean published,
			@RequestPart("file") MultipartFile file) {
		return contentService.uploadMaterial(user(userId), moduleId, title, type, published, file);
	}

	@PatchMapping("/materials/{materialId}/publish")
	public MaterialDto publish(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long materialId,
			@Valid @RequestBody PublishRequest request) {
		return contentService.setPublished(user(userId), materialId, request.published());
	}

	@GetMapping("/materials/{materialId}/file")
	public ResponseEntity<Resource> downloadMaterial(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long materialId) {
		return contentService.downloadMaterial(user(userId), materialId);
	}

	@GetMapping("/courses/{courseId}/assignments")
	public List<AssignmentDto> assignments(@RequestHeader(value = "X-User-Id", required = false) Long userId, @PathVariable Long courseId) {
		return contentService.listAssignments(user(userId), courseId);
	}

	@PostMapping("/courses/{courseId}/assignments")
	public AssignmentDto createAssignment(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long courseId,
			@Valid @RequestBody CreateAssignmentRequest request) {
		return contentService.createAssignment(user(userId), courseId, request);
	}

	@PostMapping("/assignments/{assignmentId}/submissions")
	public SubmissionDto submit(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long assignmentId,
			@RequestPart("file") MultipartFile file) {
		return contentService.submitAssignment(user(userId), assignmentId, file);
	}

	@GetMapping("/assignments/{assignmentId}/submissions")
	public List<SubmissionDto> submissions(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long assignmentId) {
		return contentService.listSubmissions(user(userId), assignmentId);
	}

	@GetMapping("/submissions/{submissionId}/file")
	public ResponseEntity<Resource> downloadSubmission(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long submissionId) {
		return contentService.downloadSubmission(user(userId), submissionId);
	}

	@PostMapping("/submissions/{submissionId}/grade")
	public SubmissionDto grade(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long submissionId,
			@Valid @RequestBody GradeSubmissionRequest request) {
		return contentService.gradeSubmission(user(userId), submissionId, request);
	}

	@GetMapping("/courses/{courseId}/announcements")
	public List<AnnouncementDto> announcements(@RequestHeader(value = "X-User-Id", required = false) Long userId, @PathVariable Long courseId) {
		return contentService.listAnnouncements(user(userId), courseId);
	}

	@PostMapping("/courses/{courseId}/announcements")
	public AnnouncementDto createAnnouncement(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long courseId,
			@Valid @RequestBody CreateAnnouncementRequest request) {
		return contentService.createAnnouncement(user(userId), courseId, request);
	}

	@GetMapping("/courses/{courseId}/discussions")
	public List<DiscussionDto> discussions(@RequestHeader(value = "X-User-Id", required = false) Long userId, @PathVariable Long courseId) {
		return contentService.listDiscussions(user(userId), courseId);
	}

	@PostMapping("/courses/{courseId}/discussions")
	public DiscussionDto createDiscussion(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long courseId,
			@Valid @RequestBody CreateDiscussionRequest request) {
		return contentService.createDiscussion(user(userId), courseId, request);
	}

	@PostMapping("/discussions/{postId}/replies")
	public DiscussionDto reply(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long postId,
			@Valid @RequestBody CreateDiscussionRequest request) {
		return contentService.reply(user(userId), postId, request);
	}

	@PutMapping("/discussions/{postId}")
	public DiscussionDto editDiscussion(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long postId,
			@Valid @RequestBody CreateDiscussionRequest request) {
		return contentService.editDiscussion(user(userId), postId, request);
	}

	@DeleteMapping("/discussions/{postId}")
	public void deleteDiscussion(
			@RequestHeader(value = "X-User-Id", required = false) Long userId,
			@PathVariable Long postId) {
		contentService.deleteDiscussion(user(userId), postId);
	}

	private User user(Long userId) {
		return lmsService.requireUser(userId);
	}
}
