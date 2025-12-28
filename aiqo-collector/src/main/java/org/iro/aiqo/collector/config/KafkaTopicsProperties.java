package org.iro.aiqo.collector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "collector.kafka")
public class KafkaTopicsProperties {

    private String collectorTopic;

    public String getCollectorTopic() {
        return collectorTopic;
    }

    public void setCollectorTopic(String collectorTopic) {
        this.collectorTopic = collectorTopic;
    }
}
