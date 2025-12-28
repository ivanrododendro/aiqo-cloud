package org.iro.aiqo.messaging;

public record LlmPayload(
        Long id,
        String modelName,
        String mainPrompt,
        String structurePrompt,
        String apiKey
) {
}
