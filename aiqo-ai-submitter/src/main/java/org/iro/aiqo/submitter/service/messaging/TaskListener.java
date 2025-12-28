package org.iro.aiqo.submitter.service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.iro.aiqo.messaging.AiTaskMessage;
import org.iro.aiqo.submitter.Submitter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskListener {

    private final Submitter submitter;

    @KafkaListener(topics = "#{@kafkaTopicsProperties.tasksTopic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onTask(AiTaskMessage message) {
        log.info("Received AI task {} from Kafka", message.taskId());
        submitter.processTask(message);
    }
}
