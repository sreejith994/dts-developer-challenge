package com.developer.challenge.tracker.dto;

import com.developer.challenge.tracker.domain.Task;
import com.developer.challenge.tracker.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "A caseworker task")
public record TaskResponse(
		Long id,
		String title,
		String description,
		TaskStatus status,
		OffsetDateTime dueDate,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {

	public static TaskResponse from(Task task) {
		return new TaskResponse(
				task.getId(),
				task.getTitle(),
				task.getDescription(),
				task.getStatus(),
				task.getDueDate(),
				task.getCreatedAt(),
				task.getUpdatedAt()
		);
	}

}
