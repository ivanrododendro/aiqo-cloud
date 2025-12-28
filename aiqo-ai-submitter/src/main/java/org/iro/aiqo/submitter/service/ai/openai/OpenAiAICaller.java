package org.iro.aiqo.submitter.service.ai.openai;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.iro.aiqo.submitter.service.ai.AICaller;
import org.iro.aiqo.submitter.service.ai.AiCallerProperties;
import org.iro.aiqo.submitter.service.ai.PromptComposer;
import org.iro.aiqo.submitter.service.ai.model.AIResult;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
public class OpenAiAICaller implements AICaller {

    private final PromptComposer promptComposer;
    private final ObjectMapper objectMapper;
    private final OpenAiChatModelFactory chatModelFactory;
    private final AiCallerProperties properties;

    public OpenAiAICaller(
            PromptComposer promptComposer,
            ObjectMapper objectMapper,
            OpenAiChatModelFactory chatModelFactory,
            AiCallerProperties properties
    ) {
        this.promptComposer = promptComposer;
        this.objectMapper = objectMapper;
        this.chatModelFactory = chatModelFactory;
        this.properties = properties;
    }

    @Override
    public AIResult call(String apiKey, String modelName, String mainPrompt, String ddlPrompt, String serverHintsPrompt, String sql, String executionPlan) {
        Assert.hasText(apiKey, "apiKey must not be blank");
        String structurePrompt = properties.getStructurePrompt();
        String payload = promptComposer.compose(mainPrompt, ddlPrompt, serverHintsPrompt, structurePrompt, sql, executionPlan);
        log.info("Dispatching prompt to OpenAI model {}", modelName);
        ChatModel chatModel = chatModelFactory.create(apiKey);
        Prompt request = buildPrompt(modelName, payload);
        ChatResponse response = chatModel.call(request);
        String content = response.getResult().getOutput().getText();
        log.debug("Received raw response: {}", content);
        return deserialize(content);
    }

    private Prompt buildPrompt(String modelName, String prompt) {
        OpenAiChatOptions options = new OpenAiChatOptions();
        options.setModel(modelName);
        List<Message> instructions = List.of(new UserMessage(prompt));
        return new Prompt(instructions, options);
    }

    private AIResult deserialize(String content) {
        try {
            return objectMapper.readValue(content, AIResult.class);
        } catch (JsonProcessingException exception) {
            log.error("Unable to deserialize OpenAI response payload: {}", content, exception);
            throw new IllegalStateException("Unable to parse OpenAI response", exception);
        }
    }
}
