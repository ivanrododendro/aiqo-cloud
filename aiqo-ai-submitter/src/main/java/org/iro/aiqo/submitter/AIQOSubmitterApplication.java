package org.iro.aiqo.submitter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableKafka
@ConfigurationPropertiesScan
public class AIQOSubmitterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AIQOSubmitterApplication.class, args);
    }
}
