package com.developer.challenge.tracker.controller;

import com.developer.challenge.tracker.dto.CreateTaskRequest;
import com.developer.challenge.tracker.dto.CreateTaskResponse;
import com.developer.challenge.tracker.dto.PagedResponse;
import com.developer.challenge.tracker.dto.TaskResponse;
import com.developer.challenge.tracker.dto.UpdateTaskStatusRequest;
import com.developer.challenge.tracker.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Create, view, update and delete caseworker tasks")
public class TaskController {

	private final TaskService taskService;

	@PostMapping
	@Operation(summary = "Create a task")
	@ApiResponse(responseCode = "201", description = "Task created; returns the new task's id")
	@ApiResponse(responseCode = "400", description = "Invalid request",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	public ResponseEntity<CreateTaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(new CreateTaskResponse(taskService.createTask(request)));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a task by id")
	@ApiResponse(responseCode = "200", description = "Task found")
	@ApiResponse(responseCode = "400", description = "Invalid id",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "Task not found",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	public ResponseEntity<TaskResponse> getTask(@PathVariable UUID id) {
		return ResponseEntity.ok(taskService.getTask(id));
	}

	@GetMapping
	@Operation(summary = "Get tasks", description = "Returns a page of tasks ordered by due date, earliest first")
	@Parameter(name = "page", in = ParameterIn.QUERY, description = "Zero-based page index",
			schema = @Schema(type = "integer", defaultValue = "0"))
	@Parameter(name = "size", in = ParameterIn.QUERY, description = "Number of tasks per page (max 100)",
			schema = @Schema(type = "integer", defaultValue = "20"))
	@ApiResponse(responseCode = "200", description = "Page of tasks")
	public ResponseEntity<PagedResponse<TaskResponse>> getTasks(
			@Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(taskService.getTasks(pageable));
	}

	@GetMapping("/all")
	@Operation(summary = "Get all tasks", description = "Returns every task, unpaged, ordered by due date, earliest first")
	@ApiResponse(responseCode = "200", description = "List of all tasks")
	public ResponseEntity<List<TaskResponse>> getAllTasks() {
		return ResponseEntity.ok(taskService.getAllTasks());
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Update the status of a task")
	@ApiResponse(responseCode = "200", description = "Status updated")
	@ApiResponse(responseCode = "400", description = "Invalid request",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "Task not found",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	public ResponseEntity<TaskResponse> updateStatus(@PathVariable UUID id,
			@Valid @RequestBody UpdateTaskStatusRequest request) {
		return ResponseEntity.ok(taskService.updateStatus(id, request.status()));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a task")
	@ApiResponse(responseCode = "204", description = "Task deleted")
	@ApiResponse(responseCode = "404", description = "Task not found",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
		taskService.deleteTask(id);
		return ResponseEntity.noContent().build();
	}

}
