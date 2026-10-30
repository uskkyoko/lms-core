package com.example.demo.web.dto.content;

import jakarta.validation.constraints.NotNull;

public record PublishRequest(@NotNull(message = "Published flag is required.") Boolean published) {
}
