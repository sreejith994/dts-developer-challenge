package com.developer.challenge.tracker.dto;

import com.developer.challenge.tracker.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

@Schema(description = "Details of the task to create")
public record CreateTaskRequest(

		@Schema(description = "Short title of the task", example = "Review case bundle")
		@NotBlank(message = "Title is required")
		@Size(max = 255, message = "Title must be at most 255 characters")
		String title,

		@Schema(description = "Optional longer description", example = "Check all evidence has been uploaded before the hearing")
		@Size(max = 2000, message = "Description must be at most 2000 characters")
		String description,

		@Schema(description = "Initial status; defaults to CREATED when omitted", example = "CREATED")
		TaskStatus status,

		@Schema(description = "When the task is due ", example = "2026-12-01T09:00:00Z")
		@NotNull(message = "Due date is required")
		@Future(message = "Due date must be in the future")
		OffsetDateTime dueDate
) {
}
