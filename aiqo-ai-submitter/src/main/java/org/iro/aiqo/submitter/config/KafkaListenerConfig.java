package org.iro.aiqo.submitter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class KafkaListenerConfig {

    private final KafkaListenerProperties listenerProperties;

    @Bean
    public FixedBackOff kafkaListenerBackOff() {
        return new FixedBackOff(0L, listenerProperties.getRetryAttempts());
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(kafkaListenerBackOff());
    }
}
