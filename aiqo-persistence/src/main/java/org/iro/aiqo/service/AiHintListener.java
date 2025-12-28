package org.iro.aiqo.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.iro.aiqo.messaging.AiHintMessage;
import org.iro.aiqo.messaging.HintPayload;
import org.iro.aiqo.model.AIHint;
import org.iro.aiqo.model.AITask;
import org.iro.aiqo.model.ProcessingStatus;
import org.iro.aiqo.model.Run;
import org.iro.aiqo.repository.AITaskRepository;
import org.iro.aiqo.repository.AiHintRepository;
import org.iro.aiqo.repository.RunRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiHintListener {

    private final AiHintRepository aiHintRepository;
    private final AITaskRepository aiTaskRepository;
    private final RunRepository runRepository;

    @KafkaListener(
            topics = "#{@kafkaTopicsProperties.hintsTopic}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = "spring.json.value.default.type=org.iro.aiqo.messaging.AiHintMessage"
    )
    @Transactional
    public void onHint(AiHintMessage message) {
        log.info("Received AI hint for task {}", message.taskId());
        AITask task = aiTaskRepository.findById(message.taskId())
                .orElseThrow(() -> new IllegalStateException("Missing task " + message.taskId()));
        Run run = runRepository.findById(message.runId())
                .orElseThrow(() -> new IllegalStateException("Missing run " + message.runId()));

        persistHints(message, run, task);
        task.setSummary(require(message.summary(), "summary"));
        task.setDiagnosis(require(message.diagnosis(), "diagnosis"));
        task.setProcessingStatus(ProcessingStatus.FINISHED);
        aiTaskRepository.save(task);
    }

    private void persistHints(AiHintMessage message, Run run, AITask task) {
        List<HintPayload> hints = Optional.ofNullable(message.hints()).orElse(List.of());
        for (HintPayload payload : hints) {
            AIHint hint = new AIHint();
            hint.setTenantId(message.tenantId());
            hint.setTitle(payload.title());
            hint.setDetail(payload.detail());
            hint.setSeverity(payload.severity());
            hint.setEstimatedImprovement(payload.estimatedImprovement());
            hint.setTested(Boolean.FALSE);
            hint.setProvenUseful(Boolean.FALSE);
            hint.setRun(run);
            hint.setAiTask(task);
            aiHintRepository.save(hint);
        }
    }

    private <T> T require(T value, String name) {
        return Objects.requireNonNull(value, "Missing required " + name + " from AI hint message");
    }
}
