package org.iro.aiqo.submitter.service.ai;

import org.iro.aiqo.submitter.service.ai.model.AIResult;

public interface AICaller {

    AIResult call(String apiKey, String modelName, String mainPrompt, String ddlPrompt, String serverHintsPrompt, String sql, String executionPlan);

}
