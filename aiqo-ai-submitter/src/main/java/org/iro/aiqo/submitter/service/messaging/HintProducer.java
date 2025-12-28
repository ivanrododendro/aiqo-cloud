package org.iro.aiqo.submitter.service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.iro.aiqo.messaging.AiHintMessage;
import org.iro.aiqo.submitter.config.KafkaTopicsProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HintProducer {

    private final KafkaTemplate<String, AiHintMessage> kafkaTemplate;
    private final KafkaTopicsProperties topicsProperties;

    public void publish(AiHintMessage hint) {
        kafkaTemplate.send(topicsProperties.getHintsTopic(), hint);
        log.debug("Published AI hint for task {} to topic {}", hint.taskId(), topicsProperties.getHintsTopic());
    }
}
