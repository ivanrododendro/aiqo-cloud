package org.iro.aiqo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan("org.iro.aiqo.model")
@EnableScheduling
@EnableKafka
@ConfigurationPropertiesScan
public class AIQOPersistenceApplication {

	public static void main(String[] args) {
		// Main method that serves as the entry point for the Spring Boot application.
		SpringApplication.run(AIQOPersistenceApplication.class, args);
	}

}
