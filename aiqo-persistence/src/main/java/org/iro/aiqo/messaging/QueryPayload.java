package org.iro.aiqo.messaging;

public record QueryPayload(
        Long id,
        String sql
) {
}
