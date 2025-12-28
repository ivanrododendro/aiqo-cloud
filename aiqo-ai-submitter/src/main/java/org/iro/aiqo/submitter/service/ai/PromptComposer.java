package org.iro.aiqo.submitter.service.ai;

import java.util.Objects;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PromptComposer {

    public String compose(String mainPrompt, String ddlPrompt, String serverHintsPrompt, String structurePrompt, String sql, String executionPlan) {
        String basePrompt = Objects.requireNonNull(mainPrompt, "functionalPrompt is required");
        String sqlSection = Objects.requireNonNull(sql, "sql is required");
        String planSection = Objects.requireNonNull(executionPlan, "executionPlan is required");

        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(structurePrompt)) {
            builder.append(structurePrompt.trim()).append("\n\n");
        }

        builder.append("### Functional Prompt ###\n")
                .append(basePrompt)
                .append("\n\n")
                .append("### EXPECTED RESPONSE STRUCTURE ###")
                .append(structurePrompt)
                .append("\n\n")
                .append("### QUERY ###\n")
                .append(sqlSection)
                .append("\n\n")
                .append("### QUERY Plan ###\n")
                .append(planSection)
                .append("### DDL ###")
                .append(ddlPrompt)
                .append("\n\n")
                .append("### SERVER OPTIMISATIONS ###")
                .append(serverHintsPrompt)
                .append("\n\n");

        return builder.toString();
    }
}
