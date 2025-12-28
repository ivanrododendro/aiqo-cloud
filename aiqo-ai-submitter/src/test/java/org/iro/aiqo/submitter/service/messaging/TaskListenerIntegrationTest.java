package org.iro.aiqo.submitter.service.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import org.iro.aiqo.messaging.AiHintMessage;
import org.iro.aiqo.messaging.AiTaskMessage;
import org.iro.aiqo.messaging.ApplicationPayload;
import org.iro.aiqo.messaging.LlmPayload;
import org.iro.aiqo.messaging.QueryPayload;
import org.iro.aiqo.messaging.RunPayload;
import org.iro.aiqo.submitter.service.ai.AICaller;
import org.iro.aiqo.submitter.service.ai.model.AIResult;
import org.iro.aiqo.submitter.service.ai.model.Hint;
import org.iro.aiqo.submitter.service.ai.model.HintSeverity;

@SpringBootTest(
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.group-id=aiqo-submitter-test",
                "aiqo.kafka.tasks-topic=aiqo.tasks.test",
                "aiqo.kafka.hints-topic=aiqo.hints.test"
        }
)
@EmbeddedKafka(partitions = 1, topics = {"aiqo.tasks.test", "aiqo.hints.test"})
@EnabledIfEnvironmentVariable(named = "ENABLE_KAFKA_TESTS", matches = "true")
class TaskListenerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void consumesTaskAndPublishesHint() {
        AiTaskMessage message = new AiTaskMessage(
                9L,
                77,
                new LlmPayload(1L, "gpt-4o", "main", "structure", "key"),
                new RunPayload(2L, "PLAN"),
                new QueryPayload(3L, "select 1"),
                new ApplicationPayload(4L, "ddl", "server")
        );

        kafkaTemplate.send("aiqo.tasks.test", message);

        Consumer<String, AiHintMessage> consumer = buildHintConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "aiqo.hints.test");
        ConsumerRecords<String, AiHintMessage> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
        assertThat(records.count()).isEqualTo(1);
        AiHintMessage hint = records.iterator().next().value();
        assertThat(hint.taskId()).isEqualTo(9L);
        assertThat(hint.tenantId()).isEqualTo(77);
        assertThat(hint.summary()).isEqualTo("summary");
        assertThat(hint.diagnosis()).isEqualTo("diagnosis");
        assertThat(hint.hints()).hasSize(1);
        assertThat(hint.hints().get(0).detail()).isEqualTo("detail");
        consumer.close();
    }

    private Consumer<String, AiHintMessage> buildHintConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "hint-consumer",
                "true",
                embeddedKafkaBroker
        );
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AiHintMessage.class.getName());
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        DefaultKafkaConsumerFactory<String, AiHintMessage> consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(AiHintMessage.class)
        );
        return consumerFactory.createConsumer();
    }

    @TestConfiguration
    static class StubConfig {
        @Bean
        @Primary
        AICaller recordingCaller() {
            return (apiKey, modelName, mainPrompt, ddlPrompt, serverHintsPrompt, sql, executionPlan) ->
                    new AIResult("summary", "diagnosis", List.of(new Hint("title", "detail", HintSeverity.HIGH, "5%")));
        }
    }
}
