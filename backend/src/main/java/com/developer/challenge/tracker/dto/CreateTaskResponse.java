package com.developer.challenge.tracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Id of newly created task")
public record CreateTaskResponse(Long id) {
}
