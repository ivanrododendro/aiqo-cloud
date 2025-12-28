package org.iro.aiqo.submitter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "aiqo.kafka.listener")
public class KafkaListenerProperties {

    private long retryAttempts = 0L;

}
