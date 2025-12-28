package org.iro.aiqo.submitter.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.iro.aiqo.submitter.service.ai.model.AIResult;
import org.iro.aiqo.submitter.service.ai.model.Hint;
import org.iro.aiqo.submitter.service.ai.model.HintSeverity;
import org.iro.aiqo.submitter.service.ai.openai.OpenAiAICaller;
import org.iro.aiqo.submitter.service.ai.openai.OpenAiChatModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;

class OpenAiAICallerTest {

    private StubChatModel chatModel;
    private TestFactory factory;
    private OpenAiAICaller caller;

    @BeforeEach
    void setUp() throws Exception {
        AiCallerProperties properties = new AiCallerProperties();
        properties.setStructurePrompt("Structure meta prompt");
        PromptComposer composer = new PromptComposer();
        ObjectMapper objectMapper = new ObjectMapper();
        AIResult response = new AIResult(
                "Summary",
                "Diagnosis",
                List.of(new Hint("title", "detail", HintSeverity.MEDIUM, "20%"))
        );
        chatModel = new StubChatModel(objectMapper.writeValueAsString(response));
        factory = new TestFactory(chatModel);
        caller = new OpenAiAICaller(composer, objectMapper, factory, properties);
    }

    @Test
    void callInvokesChatModelWithDynamicModel() {
        AIResult result = caller.call(
                "test-api-key",
                "gpt-4o",
                "Prompt",
                "CREATE TABLE t(id INT);",
                "Hint: ensure indexes",
                "SELECT 1",
                "Seq Scan"
        );

        assertThat(result.getSummary()).isEqualTo("Summary");
        assertThat(result.getDiagnosis()).isEqualTo("Diagnosis");
        assertThat(result.getHints()).hasSize(1);
        assertThat(result.getHints().get(0).getEstimatedImprovement()).isEqualTo("20%");
        OpenAiChatOptions options = (OpenAiChatOptions) chatModel.lastPrompt().getOptions();
        assertThat(options.getModel()).isEqualTo("gpt-4o");
        assertThat(chatModel.lastPrompt().getInstructions())
                .extracting(Message::getText)
                .anyMatch(text -> text.contains("Structure meta prompt"));
        assertThat(factory.lastApiKey).isEqualTo("test-api-key");
    }

    private static final class StubChatModel implements ChatModel {

        private final String jsonResponse;
        private Prompt lastPrompt;

        private StubChatModel(String jsonResponse) {
            this.jsonResponse = jsonResponse;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            this.lastPrompt = prompt;
            AssistantMessage output = new AssistantMessage(jsonResponse);
            Generation generation = new Generation(output);
            return new ChatResponse(List.of(generation));
        }

        public Prompt lastPrompt() {
            return lastPrompt;
        }
    }

    private static final class TestFactory implements OpenAiChatModelFactory {

        private final ChatModel chatModel;
        private String lastApiKey;

        private TestFactory(ChatModel chatModel) {
            this.chatModel = chatModel;
        }

        @Override
        public ChatModel create(String apiKey) {
            this.lastApiKey = apiKey;
            return chatModel;
        }
    }
}
