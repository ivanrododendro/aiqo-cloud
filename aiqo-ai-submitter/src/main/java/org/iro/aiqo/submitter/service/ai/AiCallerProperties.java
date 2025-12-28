package org.iro.aiqo.submitter.service.ai;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aiqo.ai")
public class AiCallerProperties {

    @NotBlank
    private String structurePrompt;

}
