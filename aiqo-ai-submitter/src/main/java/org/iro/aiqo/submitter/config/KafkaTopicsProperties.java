package org.iro.aiqo.submitter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aiqo.kafka")
public class KafkaTopicsProperties {

    private String tasksTopic;
    private String hintsTopic;

    public String getTasksTopic() {
        return tasksTopic;
    }

    public void setTasksTopic(String tasksTopic) {
        this.tasksTopic = tasksTopic;
    }

    public String getHintsTopic() {
        return hintsTopic;
    }

    public void setHintsTopic(String hintsTopic) {
        this.hintsTopic = hintsTopic;
    }
}
