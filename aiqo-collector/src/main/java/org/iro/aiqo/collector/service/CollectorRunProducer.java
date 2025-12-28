package org.iro.aiqo.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.iro.aiqo.collector.config.KafkaTopicsProperties;
import org.iro.aiqo.collector.messaging.CollectorRunMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorRunProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicsProperties topicsProperties;

    public void send(CollectorRunMessage message) {
        kafkaTemplate.send(topicsProperties.getCollectorTopic(), message);
        log.debug("Published collector run for hash {} to topic {}", message.hashcode(), topicsProperties.getCollectorTopic());
    }
}
