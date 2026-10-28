package com.developer.challenge.tracker.repository;

import com.developer.challenge.tracker.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
}
