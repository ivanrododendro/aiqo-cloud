package org.iro.aiqo.messaging;

import java.util.List;

public record AiHintMessage(
        Long taskId,
        Integer tenantId,
        Long runId,
        String summary,
        String diagnosis,
        List<HintPayload> hints
) {
}
