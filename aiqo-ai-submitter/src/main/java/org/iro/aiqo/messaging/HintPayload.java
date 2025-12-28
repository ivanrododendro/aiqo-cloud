package org.iro.aiqo.messaging;

public record HintPayload(
        String title,
        String detail,
        String severity,
        String estimatedImprovement
) {
}
