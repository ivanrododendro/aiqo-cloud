package org.iro.aiqo.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.iro.aiqo.config.KafkaTopicsProperties;
import org.iro.aiqo.messaging.AiTaskMessage;
import org.iro.aiqo.messaging.ApplicationPayload;
import org.iro.aiqo.messaging.LlmPayload;
import org.iro.aiqo.messaging.QueryPayload;
import org.iro.aiqo.messaging.RunPayload;
import org.iro.aiqo.model.AITask;
import org.iro.aiqo.model.Application;
import org.iro.aiqo.model.LLM;
import org.iro.aiqo.model.ProcessingStatus;
import org.iro.aiqo.model.Query;
import org.iro.aiqo.model.Run;
import org.iro.aiqo.repository.AITaskRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiTaskDispatcher {

    private final AITaskRepository taskRepository;
    private final KafkaTemplate<String, AiTaskMessage> kafkaTemplate;
    private final KafkaTopicsProperties topicsProperties;
    private boolean dispatcherEnabled = true;

    @Value("${aiqo.tasks.dispatcher.enabled:true}")
    public void setDispatcherEnabled(boolean dispatcherEnabled) {
        this.dispatcherEnabled = dispatcherEnabled;
    }

    @Scheduled(
            initialDelayString = "${aiqo.tasks.dispatcher.initial-delay-ms:5000}",
            fixedDelayString = "${aiqo.tasks.dispatcher.delay-ms:5000}"
    )

    //TODO must be sent directly after the task creation (or not?)
    public void dispatchPendingTasks() {
        if (!dispatcherEnabled) {
            log.debug("AI task dispatcher disabled via configuration");
            return;
        }
        String topic = Objects.requireNonNull(topicsProperties.getTasksTopic(), "Tasks topic not configured");
        Optional<AITask> next = taskRepository.findAndLockNextTask();
        while (next.isPresent()) {
            AITask task = next.get();
            lock(task);
            AiTaskMessage payload = buildMessage(task);
            kafkaTemplate.send(topic, payload);
            log.info("Dispatched task {} for tenant {} to Kafka topic {}", task.getId(), task.getTenantId(), topic);
            next = taskRepository.findAndLockNextTask();
        }
    }

    private void lock(AITask task) {
        task.setLockedAt(OffsetDateTime.now(ZoneOffset.UTC));
        task.setProcessingStatus(ProcessingStatus.RUNNING);
        taskRepository.save(task);
    }

    private AiTaskMessage buildMessage(AITask task) {
        Run run = Objects.requireNonNull(task.getRun(), "Task missing run");
        Query query = Objects.requireNonNull(run.getQuery(), "Run missing query");
        Application application = Objects.requireNonNull(query.getApplication(), "Query missing application");
        LLM llm = Objects.requireNonNull(application.getDefaultLLM(), "Application missing default LLM");

        return new AiTaskMessage(
                task.getId(),
                task.getTenantId(),
                new LlmPayload(
                        llm.getId(),
                        llm.getModelName(),
                        llm.getMainPrompt(),
                        llm.getStructurePrompt(),
                        llm.getApiKey()
                ),
                new RunPayload(run.getId(), run.getPlan()),
                new QueryPayload(query.getId(), query.getSql()),
                new ApplicationPayload(application.getId(), application.getDdlPrompt(), application.getEventsPrompt())
        );
    }
}
