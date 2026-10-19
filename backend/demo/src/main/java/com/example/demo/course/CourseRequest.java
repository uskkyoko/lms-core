package com.example.demo.course;

import jakarta.validation.constraints.*;
import java.util.Set;

public record CourseRequest(
    @NotBlank @Size(max = 150) String title,
    @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9 -]*", message = "Use letters, numbers, spaces or hyphens.") String code,
    @NotBlank @Size(max = 2000) String description,
    @NotBlank @Size(max = 60) String term,
    @NotEmpty Set<@NotNull @Positive Long> instructorIds
) {}
