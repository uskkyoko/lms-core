package com.example.demo.web.dto.content;

import jakarta.validation.constraints.NotNull;

public record CompleteRequest(@NotNull(message = "Completed flag is required.") Boolean completed) {
}
