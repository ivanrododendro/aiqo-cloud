package org.iro.aiqo.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.iro.aiqo.model.AITask;
import org.iro.aiqo.model.ProcessingStatus;
import org.iro.aiqo.repository.AITaskRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AITaskController {
    private final AITaskRepository taskRepository;

    public AITaskController(AITaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Operation(summary = "Get next pending task")
    @GetMapping("/tasks/next-pending")
    public ResponseEntity<AITask> nextPendingTask() {
        Optional<AITask> task = taskRepository.findAndLockNextTask();

        if (task.isPresent()) {
            AITask entity = task.get();
            entity.setLockedAt(OffsetDateTime.now(ZoneOffset.UTC));
            entity.setProcessingStatus(ProcessingStatus.FAILED);

            taskRepository.save(entity);

            return ResponseEntity.ok(entity);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
