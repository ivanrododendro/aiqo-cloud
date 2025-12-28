package org.iro.aiqo.submitter.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.FixedBackOff;

class KafkaListenerConfigTest {

    @Test
    void givenZeroRetryAttemptsWhenCreatingErrorHandlerThenBackOffStopsImmediately() {
        KafkaListenerProperties properties = new KafkaListenerProperties();
        properties.setRetryAttempts(0L);
        KafkaListenerConfig config = new KafkaListenerConfig(properties);

        FixedBackOff backOff = config.kafkaListenerBackOff();
        BackOffExecution execution = backOff.start();

        assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
    }
}
