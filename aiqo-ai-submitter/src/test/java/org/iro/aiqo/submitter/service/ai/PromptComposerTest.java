package org.iro.aiqo.submitter.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PromptComposerTest {

    @Test
    void composeConcatenatesAllSections() {
        PromptComposer composer = new PromptComposer();

        String prompt = composer.compose(
                "Do magic",
                "CREATE TABLE foo(id INT);",
                "Use covering indexes",
                "Structure JSON",
                "SELECT 1",
                "Seq Scan"
        );

        assertThat(prompt).contains("Structure JSON")
                .contains("### Functional Prompt ###")
                .contains("Do magic")
                .contains("### EXPECTED RESPONSE STRUCTURE ###")
                .contains("### QUERY ###")
                .contains("SELECT 1")
                .contains("### QUERY Plan ###")
                .contains("Seq Scan")
                .contains("### DDL ###")
                .contains("### SERVER OPTIMISATIONS ###")
                .contains("Use covering indexes");
    }
}
