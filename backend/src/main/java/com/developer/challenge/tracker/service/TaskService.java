package com.developer.challenge.tracker.service;

import com.developer.challenge.tracker.domain.Task;
import com.developer.challenge.tracker.domain.TaskStatus;
import com.developer.challenge.tracker.dto.CreateTaskRequest;
import com.developer.challenge.tracker.dto.PagedResponse;
import com.developer.challenge.tracker.dto.TaskResponse;
import com.developer.challenge.tracker.exception.TaskNotFoundException;
import com.developer.challenge.tracker.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

	private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.asc("dueDate"), Sort.Order.asc("createdAt"));

	private final TaskRepository taskRepository;

	public Long createTask(CreateTaskRequest request) {
		Task task = Task.builder()
				.title(request.title().strip())
				.description(request.description())
				.status(TaskStatus.CREATED)
				.dueDate(request.dueDate())
				.build();

		Long id = taskRepository.save(task).getId();
		log.info("Created task {}", id);
		return id;
	}

	public TaskResponse getTask(Long id) {
		return TaskResponse.from(findTask(id));
	}

	public PagedResponse<TaskResponse> getTasks(Pageable pageable) {
		Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
		Page<TaskResponse> tasks = taskRepository.findAll(sorted).map(TaskResponse::from);
		return PagedResponse.from(tasks);
	}

	public List<TaskResponse> getAllTasks() {
		return taskRepository.findAll(DEFAULT_SORT).stream()
				.map(TaskResponse::from)
				.toList();
	}

	public TaskResponse updateStatus(Long id, TaskStatus status) {
		Task task = findTask(id);
		task.setStatus(status);
		Task saved = taskRepository.saveAndFlush(task);
		log.info("Updated task {} status to {}", id, status);
		return TaskResponse.from(saved);
	}

	public void deleteTask(Long id) {
		Task task = findTask(id);
		taskRepository.delete(task);
		log.info("Deleted task {}", id);
	}

	private Task findTask(Long id) {
		return taskRepository.findById(id)
				.orElseThrow(() -> new TaskNotFoundException(id));
	}

}
