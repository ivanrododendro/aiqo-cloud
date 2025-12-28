package org.iro.aiqo.submitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.iro.aiqo.messaging.AiTaskMessage;
import org.iro.aiqo.messaging.ApplicationPayload;
import org.iro.aiqo.messaging.LlmPayload;
import org.iro.aiqo.messaging.QueryPayload;
import org.iro.aiqo.messaging.RunPayload;
import org.iro.aiqo.submitter.service.ai.AICaller;
import org.iro.aiqo.submitter.service.ai.model.AIResult;
import org.iro.aiqo.submitter.service.ai.model.Hint;
import org.iro.aiqo.submitter.service.ai.model.HintSeverity;
import org.iro.aiqo.submitter.service.messaging.HintProducer;
import org.junit.jupiter.api.Test;

class SubmitterTest {

    @Test
    void processTaskPublishesHintUsingAiResult() {
        RecordingAICaller aiCaller = new RecordingAICaller();
        HintProducer hintProducer = mock(HintProducer.class);
        Submitter submitter = new Submitter(aiCaller, hintProducer);
        AiTaskMessage message = buildMessage();

        submitter.processTask(message);

        assertThat(aiCaller.invocationCount).isEqualTo(1);
        assertThat(aiCaller.lastSql).isEqualTo("select 1");
        assertThat(aiCaller.lastPlan).isEqualTo("PLAN");
        verify(hintProducer).publish(any());
    }

    @Test
    void processTaskFailsWhenMainPromptMissing() {
        RecordingAICaller aiCaller = new RecordingAICaller();
        HintProducer hintProducer = mock(HintProducer.class);
        Submitter submitter = new Submitter(aiCaller, hintProducer);
        AiTaskMessage message = new AiTaskMessage(
                1L,
                7,
                new LlmPayload(1L, null, null, "structure", "key"),
                new RunPayload(2L, "PLAN"),
                new QueryPayload(3L, "select 1"),
                new ApplicationPayload(4L, "ddl", "server")
        );

        assertThatThrownBy(() -> submitter.processTask(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LLM main prompt");
    }

    private AiTaskMessage buildMessage() {
        return new AiTaskMessage(
                1L,
                7,
                new LlmPayload(1L, "gpt-4o", "main", "structure", "key"),
                new RunPayload(2L, "PLAN"),
                new QueryPayload(3L, "select 1"),
                new ApplicationPayload(4L, "ddl", "server")
        );
    }

    private static class RecordingAICaller implements AICaller {

        private int invocationCount = 0;
        private String lastSql;
        private String lastPlan;

        @Override
        public AIResult call(String apiKey, String modelName, String mainPrompt, String ddlPrompt, String serverHintsPrompt, String sql, String executionPlan) {
            invocationCount++;
            lastSql = sql;
            lastPlan = executionPlan;
            return new AIResult("summary", "diagnosis", List.of(new Hint("title", "detail", HintSeverity.LOW, "10%")));
        }
    }
}
