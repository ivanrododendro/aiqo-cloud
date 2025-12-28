package org.iro.aiqo.messaging;

public record AiTaskMessage(
        Long taskId,
        Integer tenantId,
        LlmPayload llm,
        RunPayload run,
        QueryPayload query,
        ApplicationPayload application
) {
}
