package org.iro.aiqo.submitter;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.iro.aiqo.messaging.AiHintMessage;
import org.iro.aiqo.messaging.AiTaskMessage;
import org.iro.aiqo.messaging.ApplicationPayload;
import org.iro.aiqo.messaging.HintPayload;
import org.iro.aiqo.messaging.LlmPayload;
import org.iro.aiqo.messaging.QueryPayload;
import org.iro.aiqo.messaging.RunPayload;
import org.iro.aiqo.submitter.service.ai.AICaller;
import org.iro.aiqo.submitter.service.ai.model.AIResult;
import org.iro.aiqo.submitter.service.ai.model.Hint;
import org.iro.aiqo.submitter.service.messaging.HintProducer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class Submitter {

    private final AICaller aiCaller;
    private final HintProducer hintProducer;

    public void processTask(AiTaskMessage task) {
        log.info("Processing AI task {}", task.taskId());
        RunPayload run = requireNonNull(task.run(), "AI task missing run");
        QueryPayload query = requireNonNull(task.query(), "AI task missing query");
        ApplicationPayload application = requireNonNull(task.application(), "AI task missing application");
        LlmPayload llm = requireNonNull(task.llm(), "AI task missing llm payload");

        String sql = requireText(query.sql(), "Query SQL is required to contact the LLM");
        String executionPlan = requireText(run.plan(), "Execution plan is required to contact the LLM");
        String mainPrompt = requireText(llm.mainPrompt(), "LLM main prompt not configured");
        String ddlPrompt = application.ddlPrompt();
        String serverHintsPrompt = application.serverHintsPrompt();
        String apiKey = requireText(llm.apiKey(), "LLM API key not configured");
        String modelName = requireText(llm.modelName(), "LLM model name not configured");

        AIResult aiResult = aiCaller.call(apiKey, modelName, mainPrompt, ddlPrompt, serverHintsPrompt, sql, executionPlan);
        AiHintMessage hint = composeHint(task, query, aiResult);
        hintProducer.publish(hint);
        log.info("Created AI hint for task {}", task.taskId());
    }

    private AiHintMessage composeHint(AiTaskMessage task, QueryPayload query, AIResult result) {
        return new AiHintMessage(
                task.taskId(),
                task.tenantId(),
                task.run().id(),
                pickSummary(result, query),
                pickDiagnosis(result, query),
                buildHints(result)
        );
    }

    private List<HintPayload> buildHints(AIResult result) {
        List<Hint> hints = Optional.ofNullable(result.getHints()).orElse(emptyList());
        return hints.stream()
                .map(this::toPayload)
                .toList();
    }

    private HintPayload toPayload(Hint hint) {
        String severity = hint.getSeverity() != null ? hint.getSeverity().name() : null;
        return new HintPayload(
                hint.getTitle(),
                hint.getDetail(),
                severity,
                hint.getEstimatedImprovement()
        );
    }

    private String pickSummary(AIResult result, QueryPayload fallbackQuery) {
        if (StringUtils.hasText(result.getSummary())) {
            return result.getSummary();
        }
        List<Hint> hints = result.getHints();
        if (hints != null && !hints.isEmpty() && StringUtils.hasText(hints.get(0).getDetail())) {
            return hints.get(0).getDetail();
        }
        return Optional.ofNullable(fallbackQuery.sql()).filter(StringUtils::hasText).orElse("Summary not available");
    }

    private String pickDiagnosis(AIResult result, QueryPayload fallbackQuery) {
        if (StringUtils.hasText(result.getDiagnosis())) {
            return result.getDiagnosis();
        }
        if (StringUtils.hasText(result.getSummary())) {
            return result.getSummary();
        }
        List<Hint> hints = result.getHints();
        if (hints != null && !hints.isEmpty() && StringUtils.hasText(hints.get(0).getDetail())) {
            return hints.get(0).getDetail();
        }
        return Optional.ofNullable(fallbackQuery.sql()).filter(StringUtils::hasText).orElse("Diagnosis not available");
    }

    private <T> T requireNonNull(T value, String message) {
        return Objects.requireNonNull(value, message);
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value;
    }
}
