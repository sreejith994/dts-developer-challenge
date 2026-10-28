package com.developer.challenge.tracker.repository;

import com.developer.challenge.tracker.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
