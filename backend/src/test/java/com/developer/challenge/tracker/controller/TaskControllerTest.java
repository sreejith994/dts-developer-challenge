package com.developer.challenge.tracker.controller;

import com.developer.challenge.tracker.domain.TaskStatus;
import com.developer.challenge.tracker.dto.CreateTaskRequest;
import com.developer.challenge.tracker.dto.PagedResponse;
import com.developer.challenge.tracker.dto.TaskResponse;
import com.developer.challenge.tracker.exception.TaskNotFoundException;
import com.developer.challenge.tracker.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

	private static final Long TASK_ID = 42L;
	private static final OffsetDateTime DUE_DATE = OffsetDateTime.now(ZoneOffset.UTC).plusYears(1).truncatedTo(ChronoUnit.SECONDS);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TaskService taskService;

	@Test
	void createTask_returns201WithId() throws Exception {
		when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(TASK_ID);

		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": "Review bundle", "description": "Check evidence", "dueDate": "%s"}
								""".formatted(DUE_DATE)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(TASK_ID));
	}

	@Test
	void createTask_returns400WithFieldErrorsWhenInvalid() throws Exception {
		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": " ", "dueDate": "2000-01-01T10:00:00Z"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Validation error"))
				.andExpect(jsonPath("$.errors.title").value("Title is required"))
				.andExpect(jsonPath("$.errors.dueDate").value("Due date must be in the future"));

		verifyNoInteractions(taskService);
	}

	@Test
	void createTask_returns400WhenDueDateMissing() throws Exception {
		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": "Review bundle"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.dueDate").value("Due date is required"));
	}

	@Test
	void getTask_returnsTask() throws Exception {
		when(taskService.getTask(TASK_ID)).thenReturn(response(TaskStatus.BLOCKED));

		mockMvc.perform(get("/api/tasks/{id}", TASK_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(TASK_ID))
				.andExpect(jsonPath("$.status").value("BLOCKED"));
	}

	@Test
	void getTask_returns404WhenNotFound() throws Exception {
		when(taskService.getTask(TASK_ID)).thenThrow(new TaskNotFoundException(TASK_ID));

		mockMvc.perform(get("/api/tasks/{id}", TASK_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Task not found"))
				.andExpect(jsonPath("$.detail").value("Task with id " + TASK_ID + " not found"));
	}

	@Test
	void getTask_returns400WhenIdIsNotANumber() throws Exception {
		mockMvc.perform(get("/api/tasks/{id}", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid parameter"));
	}

	@Test
	void getTasks_returnsPage() throws Exception {
		when(taskService.getTasks(PageRequest.of(1, 5)))
				.thenReturn(new PagedResponse<>(List.of(response(TaskStatus.CREATED)), 1, 5, 6, 2));

		mockMvc.perform(get("/api/tasks").param("page", "1").param("size", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].id").value(TASK_ID))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(5))
				.andExpect(jsonPath("$.totalElements").value(6))
				.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void getTasks_usesDefaultPaging() throws Exception {
		when(taskService.getTasks(any(Pageable.class))).thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0));

		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty());

		verify(taskService).getTasks(PageRequest.of(0, 20));
	}

	@Test
	void getTasks_capsPageSizeAt100() throws Exception {
		when(taskService.getTasks(any(Pageable.class))).thenReturn(new PagedResponse<>(List.of(), 0, 100, 0, 0));

		mockMvc.perform(get("/api/tasks").param("size", "500"))
				.andExpect(status().isOk());

		verify(taskService).getTasks(PageRequest.of(0, 100));
	}

	@Test
	void getAllTasks_returnsList() throws Exception {
		when(taskService.getAllTasks()).thenReturn(List.of(response(TaskStatus.CREATED)));

		mockMvc.perform(get("/api/tasks/all"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(TASK_ID));
	}

	@Test
	void updateStatus_returnsUpdatedTask() throws Exception {
		when(taskService.updateStatus(TASK_ID, TaskStatus.COMPLETED)).thenReturn(response(TaskStatus.COMPLETED));

		mockMvc.perform(patch("/api/tasks/{id}/status", TASK_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status": "COMPLETED"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	@Test
	void updateStatus_returns400WhenStatusMissing() throws Exception {
		mockMvc.perform(patch("/api/tasks/{id}/status", TASK_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.status").value("Status is required"));

		verifyNoInteractions(taskService);
	}

	@Test
	void updateStatus_returns404WhenNotFound() throws Exception {
		when(taskService.updateStatus(TASK_ID, TaskStatus.BLOCKED)).thenThrow(new TaskNotFoundException(TASK_ID));

		mockMvc.perform(patch("/api/tasks/{id}/status", TASK_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status": "BLOCKED"}
								"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteTask_returns204() throws Exception {
		mockMvc.perform(delete("/api/tasks/{id}", TASK_ID))
				.andExpect(status().isNoContent());

		verify(taskService).deleteTask(TASK_ID);
	}

	@Test
	void deleteTask_returns404WhenNotFound() throws Exception {
		doThrow(new TaskNotFoundException(TASK_ID)).when(taskService).deleteTask(TASK_ID);

		mockMvc.perform(delete("/api/tasks/{id}", TASK_ID))
				.andExpect(status().isNotFound());
	}

	private static TaskResponse response(TaskStatus status) {
		OffsetDateTime now = OffsetDateTime.parse("2026-10-28T12:00:00Z");
		return new TaskResponse(TASK_ID, "Review bundle", "Check evidence", status, DUE_DATE, now, now);
	}

}
