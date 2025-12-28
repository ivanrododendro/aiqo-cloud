package org.iro.aiqo.submitter.service.ai.openai;

import org.springframework.ai.chat.model.ChatModel;

public interface OpenAiChatModelFactory {

    ChatModel create(String apiKey);
}
