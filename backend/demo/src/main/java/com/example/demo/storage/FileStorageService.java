package com.example.demo.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

	private final Path root;

	public FileStorageService(@Value("${lms.upload-dir}") String uploadDir) throws IOException {
		this.root = Path.of(uploadDir).toAbsolutePath().normalize();
		Files.createDirectories(this.root);
	}

	public StoredFile store(MultipartFile file, Set<String> allowedExtensions, String rejectedMessage) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("A file is required.");
		}
		String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
		String extension = extensionOf(original);
		if (!allowedExtensions.contains(extension)) {
			throw new IllegalArgumentException(rejectedMessage);
		}
		String storedName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
		Path target = root.resolve(storedName);
		try (InputStream input = file.getInputStream()) {
			Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			throw new IllegalArgumentException("The file could not be saved. Try again.");
		}
		return new StoredFile(storedName, original, file.getContentType());
	}

	public Resource load(String storedFileName) {
		Path path = root.resolve(storedFileName).normalize();
		if (!path.startsWith(root) || !Files.exists(path)) {
			throw new IllegalArgumentException("File not found.");
		}
		return new FileSystemResource(path);
	}

	public void deleteQuietly(String storedFileName) {
		if (storedFileName == null || storedFileName.isBlank()) {
			return;
		}
		try {
			Files.deleteIfExists(root.resolve(storedFileName).normalize());
		} catch (IOException ignored) {
			// leftover files are acceptable for this prototype
		}
	}

	public static String extensionOf(String filename) {
		int dot = filename.lastIndexOf('.');
		if (dot < 0 || dot == filename.length() - 1) {
			return "";
		}
		return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
	}
}
