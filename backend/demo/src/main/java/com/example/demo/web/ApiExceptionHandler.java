package com.example.demo.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> status(ResponseStatusException ex) {
		return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",
				ex.getReason() == null || ex.getReason().isBlank() ? "The request could not be completed." : ex.getReason()));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Map<String, String>> missing(NoResourceFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Map.of("message", "This action is not available. Restart the API server so it matches the current UI."));
	}
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, String>> conflict(DataIntegrityViolationException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "This change conflicts with an existing record. Course codes and enrollments must be unique; linked learning records must be retained."));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<Map<String, String>> forbidden(ForbiddenException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<Map<String, String>> missingFile(MissingServletRequestPartException ex) {
		return ResponseEntity.badRequest().body(Map.of("message", "A file is required."));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> tooLarge(MaxUploadSizeExceededException ex) {
		return ResponseEntity.badRequest().body(Map.of("message", "The file is too large. The maximum size is 20 MB."));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getDefaultMessage())
				.orElse("Required information is missing.");
		return ResponseEntity.badRequest().body(Map.of("message", message));
	}
}
