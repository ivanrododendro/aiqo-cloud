package org.iro.aiqo.submitter.service.ai.openai;

import java.util.Collections;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.retry.support.RetryTemplate;

@Component
class DefaultOpenAiChatModelFactory implements OpenAiChatModelFactory {

    private final RestClient.Builder restClientBuilder;
    private final WebClient.Builder webClientBuilder;
    private final ObjectProvider<ResponseErrorHandler> responseErrorHandlerProvider;
    private final ObservationRegistry observationRegistry;
    private final ToolCallingManager toolCallingManager;
    private final RetryTemplate retryTemplate;
    private final ToolExecutionEligibilityPredicate eligibilityPredicate;

    DefaultOpenAiChatModelFactory(
            RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder,
            ObjectProvider<ResponseErrorHandler> responseErrorHandlerProvider,
            ObjectProvider<ObservationRegistry> observationRegistryProvider
    ) {
        this.restClientBuilder = restClientBuilder;
        this.webClientBuilder = webClientBuilder;
        this.responseErrorHandlerProvider = responseErrorHandlerProvider;
        this.observationRegistry = observationRegistryProvider.getIfAvailable(ObservationRegistry::create);
        ObservationRegistry registry = this.observationRegistry != null
                ? this.observationRegistry
                : ObservationRegistry.create();
        this.toolCallingManager = DefaultToolCallingManager.builder()
                .observationRegistry(registry)
                .toolCallbackResolver(new StaticToolCallbackResolver(Collections.emptyList()))
                .toolExecutionExceptionProcessor(new DefaultToolExecutionExceptionProcessor.Builder().build())
                .build();
        this.retryTemplate = RetryTemplate.builder().maxAttempts(1).build();
        this.eligibilityPredicate = new DefaultToolExecutionEligibilityPredicate();
    }

    @Override
    public ChatModel create(String apiKey) {
        Assert.hasText(apiKey, "apiKey must not be blank");
        OpenAiApi api = buildApi(apiKey);
        OpenAiChatOptions options = new OpenAiChatOptions();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .toolCallingManager(toolCallingManager)
                .toolExecutionEligibilityPredicate(eligibilityPredicate)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry != null ? observationRegistry : ObservationRegistry.create())
                .build();
    }

    private OpenAiApi buildApi(String apiKey) {
        ResponseErrorHandler errorHandler = responseErrorHandlerProvider
                .getIfAvailable(DefaultResponseErrorHandler::new);
        return OpenAiApi.builder()
                .apiKey(new SimpleApiKey(apiKey))
                .headers(new LinkedMultiValueMap<>())
                .completionsPath("/v1/chat/completions")
                .embeddingsPath("/v1/embeddings")
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .responseErrorHandler(errorHandler)
                .build();
    }
}
