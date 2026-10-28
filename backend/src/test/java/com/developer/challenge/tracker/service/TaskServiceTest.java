package com.developer.challenge.tracker.service;

import com.developer.challenge.tracker.domain.Task;
import com.developer.challenge.tracker.domain.TaskStatus;
import com.developer.challenge.tracker.dto.CreateTaskRequest;
import com.developer.challenge.tracker.dto.PagedResponse;
import com.developer.challenge.tracker.dto.TaskResponse;
import com.developer.challenge.tracker.exception.TaskNotFoundException;
import com.developer.challenge.tracker.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	private static final UUID TASK_ID = UUID.randomUUID();
	private static final OffsetDateTime DUE_DATE = OffsetDateTime.parse("2030-01-15T10:00:00Z");

	@Mock
	private TaskRepository taskRepository;

	@InjectMocks
	private TaskService taskService;

	@Test
	void createTask_savesTaskAndReturnsId() {
		stubSaveAssigningId();

		UUID id = taskService.createTask(
				new CreateTaskRequest("  Review bundle  ", "Check evidence", TaskStatus.INPROGRESS, DUE_DATE));

		assertThat(id).isEqualTo(TASK_ID);

		Task saved = capturedSavedTask();
		assertThat(saved.getTitle()).isEqualTo("Review bundle");
		assertThat(saved.getDescription()).isEqualTo("Check evidence");
		assertThat(saved.getStatus()).isEqualTo(TaskStatus.INPROGRESS);
		assertThat(saved.getDueDate()).isEqualTo(DUE_DATE);
	}

	@Test
	void createTask_defaultsStatusToCreatedWhenOmitted() {
		stubSaveAssigningId();

		taskService.createTask(new CreateTaskRequest("Review bundle", null, null, DUE_DATE));

		Task saved = capturedSavedTask();
		assertThat(saved.getStatus()).isEqualTo(TaskStatus.CREATED);
		assertThat(saved.getDescription()).isNull();
	}

	@Test
	void getTask_returnsTaskWhenFound() {
		when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task(TaskStatus.CREATED)));

		TaskResponse response = taskService.getTask(TASK_ID);

		assertThat(response.id()).isEqualTo(TASK_ID);
		assertThat(response.status()).isEqualTo(TaskStatus.CREATED);
	}

	@Test
	void getTask_throwsWhenNotFound() {
		when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> taskService.getTask(TASK_ID))
				.isInstanceOf(TaskNotFoundException.class)
				.hasMessageContaining(TASK_ID.toString());
	}

	@Test
	void getTasks_appliesDefaultSortAndMapsResult() {
		when(taskRepository.findAll(any(Pageable.class))).thenAnswer(invocation -> {
			Pageable pageable = invocation.getArgument(0);
			return new PageImpl<>(List.of(task(TaskStatus.CREATED)), pageable, 11);
		});

		PagedResponse<TaskResponse> tasks = taskService.getTasks(PageRequest.of(2, 5, Sort.by("title")));

		assertThat(tasks.content()).extracting(TaskResponse::id).containsExactly(TASK_ID);
		assertThat(tasks.page()).isEqualTo(2);
		assertThat(tasks.size()).isEqualTo(5);
		assertThat(tasks.totalElements()).isEqualTo(11);
		assertThat(tasks.totalPages()).isEqualTo(3);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(taskRepository).findAll(pageableCaptor.capture());
		assertThat(pageableCaptor.getValue()).isEqualTo(
				PageRequest.of(2, 5, Sort.by(Sort.Order.asc("dueDate"), Sort.Order.asc("createdAt"))));
	}

	@Test
	void getAllTasks_returnsTasksSortedByDueDate() {
		when(taskRepository.findAll(any(Sort.class))).thenReturn(List.of(task(TaskStatus.CREATED)));

		List<TaskResponse> tasks = taskService.getAllTasks();

		assertThat(tasks).extracting(TaskResponse::id).containsExactly(TASK_ID);
		verify(taskRepository).findAll(Sort.by(Sort.Order.asc("dueDate"), Sort.Order.asc("createdAt")));
	}

	@Test
	void updateStatus_changesStatusAndSaves() {
		Task existing = task(TaskStatus.CREATED);
		when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(existing));
		when(taskRepository.saveAndFlush(existing)).thenReturn(existing);

		TaskResponse response = taskService.updateStatus(TASK_ID, TaskStatus.COMPLETED);

		assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
		assertThat(existing.getStatus()).isEqualTo(TaskStatus.COMPLETED);
	}

	@Test
	void updateStatus_throwsWhenNotFound() {
		when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> taskService.updateStatus(TASK_ID, TaskStatus.BLOCKED))
				.isInstanceOf(TaskNotFoundException.class);
		verify(taskRepository, never()).saveAndFlush(any());
	}

	@Test
	void deleteTask_deletesExistingTask() {
		Task existing = task(TaskStatus.CREATED);
		when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(existing));

		taskService.deleteTask(TASK_ID);

		verify(taskRepository).delete(existing);
	}

	@Test
	void deleteTask_throwsWhenNotFound() {
		when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> taskService.deleteTask(TASK_ID))
				.isInstanceOf(TaskNotFoundException.class);
		verify(taskRepository, never()).delete(any());
	}

	private void stubSaveAssigningId() {
		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
			Task task = invocation.getArgument(0);
			task.setId(TASK_ID);
			return task;
		});
	}

	private Task capturedSavedTask() {
		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).save(captor.capture());
		return captor.getValue();
	}

	private static Task task(TaskStatus status) {
		Task task = Task.builder()
				.title("Review bundle")
				.status(status)
				.dueDate(DUE_DATE)
				.build();
		task.setId(TASK_ID);
		return task;
	}

}
