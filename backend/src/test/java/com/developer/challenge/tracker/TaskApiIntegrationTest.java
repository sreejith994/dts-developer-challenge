package com.developer.challenge.tracker;

import com.developer.challenge.tracker.domain.TaskStatus;
import com.developer.challenge.tracker.dto.CreateTaskResponse;
import com.developer.challenge.tracker.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for API over real HTTP on a random port, using testcontainers postgres DB
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class TaskApiIntegrationTest {

	private static final OffsetDateTime NOW = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);

	@Autowired
	private RestTestClient client;

	@Autowired
	private TaskRepository taskRepository;

	@BeforeEach
	void cleanDatabase() {
		taskRepository.deleteAll();
	}

	@Test
	void taskLifecycle() {
		CreateTaskResponse created = client.post().uri("/api/tasks")
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"title": "Review bundle", "description": "Check evidence", "dueDate": "%s"}
						""".formatted(NOW.plusYears(1)))
				.exchange()
				.expectStatus().isCreated()
				.expectBody(CreateTaskResponse.class)
				.returnResult().getResponseBody();

		assertThat(created).isNotNull();
		Long id = created.id();

		client.get().uri("/api/tasks/{id}", id)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.title").isEqualTo("Review bundle")
				.jsonPath("$.description").isEqualTo("Check evidence")
				.jsonPath("$.status").isEqualTo("CREATED")
				.jsonPath("$.createdAt").isNotEmpty();

		client.patch().uri("/api/tasks/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"status": "INPROGRESS"}
						""")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("INPROGRESS");

		assertThat(taskRepository.findById(id)).get()
				.extracting("status").isEqualTo(TaskStatus.INPROGRESS);

		client.delete().uri("/api/tasks/{id}", id)
				.exchange()
				.expectStatus().isNoContent();

		client.get().uri("/api/tasks/{id}", id)
				.exchange()
				.expectStatus().isNotFound();
	}

	@Test
	void createTask_ignoresStatusInRequestAndStartsAsCreated() {
		CreateTaskResponse created = client.post().uri("/api/tasks")
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"title": "Review bundle", "status": "COMPLETED", "dueDate": "%s"}
						""".formatted(NOW.plusDays(1)))
				.exchange()
				.expectStatus().isCreated()
				.expectBody(CreateTaskResponse.class)
				.returnResult().getResponseBody();

		assertThat(created).isNotNull();
		assertThat(taskRepository.findById(created.id())).get()
				.extracting("status").isEqualTo(TaskStatus.CREATED);
	}

	@Test
	void getTasks_returnsPagesOrderedByDueDate() {
		createTask("Latest task", NOW.plusMonths(6));
		createTask("Soonest task", NOW.plusDays(1));
		createTask("Middle task", NOW.plusMonths(1));

		client.get().uri("/api/tasks?size=2")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.content.length()").isEqualTo(2)
				.jsonPath("$.content[0].title").isEqualTo("Soonest task")
				.jsonPath("$.content[1].title").isEqualTo("Middle task")
				.jsonPath("$.page").isEqualTo(0)
				.jsonPath("$.totalElements").isEqualTo(3)
				.jsonPath("$.totalPages").isEqualTo(2);

		client.get().uri("/api/tasks?page=1&size=2")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.content.length()").isEqualTo(1)
				.jsonPath("$.content[0].title").isEqualTo("Latest task")
				.jsonPath("$.page").isEqualTo(1);
	}

	@Test
	void getAllTasks_returnsEveryTaskOrderedByDueDate() {
		createTask("Latest task", NOW.plusMonths(6));
		createTask("Soonest task", NOW.plusDays(1));
		createTask("Middle task", NOW.plusMonths(1));

		client.get().uri("/api/tasks/all")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(3)
				.jsonPath("$[0].title").isEqualTo("Soonest task")
				.jsonPath("$[1].title").isEqualTo("Middle task")
				.jsonPath("$[2].title").isEqualTo("Latest task");
	}

	@Test
	void createTask_withInvalidRequest_returnsValidationErrorAndSavesNothing() {
		client.post().uri("/api/tasks")
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"title": " ", "dueDate": "2000-01-01T09:00:00Z"}
						""")
				.exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.title").isEqualTo("Validation error")
				.jsonPath("$.errors.title").isEqualTo("Title is required")
				.jsonPath("$.errors.dueDate").isEqualTo("Due date must be in the future");

		assertThat(taskRepository.count()).isZero();
	}

	@Test
	void operationsOnUnknownTask_returnNotFound() {
		Long unknownId = Long.MAX_VALUE;

		client.get().uri("/api/tasks/{id}", unknownId)
				.exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.title").isEqualTo("Task not found")
				.jsonPath("$.detail").isEqualTo("Task with id " + unknownId + " not found");

		client.patch().uri("/api/tasks/{id}/status", unknownId)
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"status": "COMPLETED"}
						""")
				.exchange()
				.expectStatus().isNotFound();

		client.delete().uri("/api/tasks/{id}", unknownId)
				.exchange()
				.expectStatus().isNotFound();

		assertThat(taskRepository.count()).isZero();
	}

	private void createTask(String title, OffsetDateTime dueDate) {
		client.post().uri("/api/tasks")
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"title": "%s", "dueDate": "%s"}
						""".formatted(title, dueDate))
				.exchange()
				.expectStatus().isCreated();
	}

}
