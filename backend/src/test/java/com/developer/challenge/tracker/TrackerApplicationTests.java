package com.developer.challenge.tracker;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the application starts against the testcontainers postgres DB with the Flyway migrations applied
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TrackerApplicationTests {

	@Autowired
	private PostgreSQLContainer postgresContainer;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private Flyway flyway;

	@Autowired
	private JdbcTemplate jdbcTemplate;


	@Test
	void postgresContainerIsRunningAndUsedByDataSource() throws Exception {
		assertThat(postgresContainer.isCreated()).isTrue();
		assertThat(postgresContainer.isRunning()).isTrue();

		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection.isValid(1)).isTrue();
			assertThat(connection.getMetaData().getURL()).startsWith(postgresContainer.getJdbcUrl());
		}
	}

	@Test
	void flywayAppliedAllMigrations() {
		MigrationInfo[] applied = flyway.info().applied();

		assertThat(applied).isNotEmpty();
		assertThat(applied).allSatisfy(migration -> assertThat(migration.getState().isFailed()).isFalse());
		assertThat(flyway.info().pending()).isEmpty();
		assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
	}

	@Test
	void flywayCreatedTaskTable() {
		Integer tables = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'task'",
				Integer.class);

		assertThat(tables).isEqualTo(1);
	}

}
