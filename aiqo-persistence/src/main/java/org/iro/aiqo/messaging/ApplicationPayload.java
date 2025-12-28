package org.iro.aiqo.messaging;

public record ApplicationPayload(
        Long id,
        String ddlPrompt,
        String serverHintsPrompt
) {
}
