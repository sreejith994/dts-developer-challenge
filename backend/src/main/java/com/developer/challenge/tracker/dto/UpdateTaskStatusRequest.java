package com.developer.challenge.tracker.dto;

import com.developer.challenge.tracker.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "New status for a task")
public record UpdateTaskStatusRequest(

		@Schema(example = "INPROGRESS")
		@NotNull(message = "Status is required")
		TaskStatus status
) {
}
