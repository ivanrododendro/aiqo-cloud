package org.iro.aiqo.submitter.service.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiCallerProperties.class)
public class AiConfiguration {
}
