package org.iro.aiqo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import org.iro.aiqo.messaging.AiHintMessage;
import org.iro.aiqo.messaging.AiTaskMessage;
import org.iro.aiqo.messaging.HintPayload;
import org.iro.aiqo.model.AIHint;
import org.iro.aiqo.model.AITask;
import org.iro.aiqo.model.Application;
import org.iro.aiqo.model.DBType;
import org.iro.aiqo.model.Environment;
import org.iro.aiqo.model.EnvironmentType;
import org.iro.aiqo.model.LLM;
import org.iro.aiqo.model.LLMFamily;
import org.iro.aiqo.model.LLMVendor;
import org.iro.aiqo.model.ProcessingStatus;
import org.iro.aiqo.model.Query;
import org.iro.aiqo.model.Run;
import org.iro.aiqo.repository.AITaskRepository;
import org.iro.aiqo.repository.AiHintRepository;
import org.iro.aiqo.repository.ApplicationRepository;
import org.iro.aiqo.repository.EnvironnementRepository;
import org.iro.aiqo.repository.LLMRepository;
import org.iro.aiqo.repository.QueryRepository;
import org.iro.aiqo.repository.RunRepository;

@SpringBootTest(
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.group-id=aiqo-persistence-test",
                "aiqo.kafka.tasks-topic=aiqo.tasks.test",
                "aiqo.kafka.hints-topic=aiqo.hints.test",
                "aiqo.tasks.dispatcher.initial-delay-ms=0",
                "aiqo.tasks.dispatcher.delay-ms=1000",
                "aiqo.tasks.dispatcher.enabled=true"
        }
)
@EmbeddedKafka(partitions = 1, topics = {"aiqo.tasks.test", "aiqo.hints.test"})
@EnabledIfEnvironmentVariable(named = "ENABLE_KAFKA_TESTS", matches = "true")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=aiqo"
})
class AiKafkaIntegrationTest {

    @Autowired
    private AiTaskDispatcher dispatcher;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AITaskRepository taskRepository;

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private QueryRepository queryRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EnvironnementRepository environnementRepository;

    @Autowired
    private LLMRepository llmRepository;

    @Autowired
    private AiHintRepository hintRepository;

    private static final Integer TENANT_ID = 42;

    @BeforeEach
    void ensureSchema() {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS aiqo");
    }

    @Test
    void dispatcherPublishesNextPendingTask() {
        AITask task = persistTaskGraph();

        dispatcher.dispatchPendingTasks();

        Consumer<String, AiTaskMessage> consumer = buildTaskConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "aiqo.tasks.test");
        ConsumerRecords<String, AiTaskMessage> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThanOrEqualTo(1);
        AiTaskMessage message = records.iterator().next().value();
        assertThat(message.taskId()).isEqualTo(task.getId());
        assertThat(message.run().plan()).isEqualTo("PLAN");
        assertThat(message.query().sql()).contains("SELECT");
        consumer.close();
    }

    @Test
    @Transactional
    void hintListenerPersistsIncomingMessage() {
        AITask task = persistTaskGraph();
        Run run = task.getRun();

        AiHintMessage hintMessage = new AiHintMessage(
                task.getId(),
                TENANT_ID,
                run.getId(),
                "summary",
                "diagnosis",
                List.of(new HintPayload("title", "detail", "HIGH", "5%"))
        );

        kafkaTemplate.send("aiqo.hints.test", hintMessage);

        waitForHints(1);
        assertThat(hintRepository.count()).isEqualTo(1);
        AIHint hint = hintRepository.findAll().get(0);
        assertThat(hint.getTitle()).isEqualTo("title");
        assertThat(hint.getSeverity()).isEqualTo("HIGH");
        AITask refreshed = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(refreshed.getProcessingStatus()).isEqualTo(ProcessingStatus.FINISHED);
        assertThat(refreshed.getSummary()).isEqualTo("summary");
        assertThat(refreshed.getDiagnosis()).isEqualTo("diagnosis");
    }

    private Consumer<String, AiTaskMessage> buildTaskConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "task-consumer",
                "true",
                embeddedKafkaBroker
        );
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AiTaskMessage.class.getName());
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        DefaultKafkaConsumerFactory<String, AiTaskMessage> consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(AiTaskMessage.class)
        );
        return consumerFactory.createConsumer();
    }

    private void waitForHints(int expectedCount) {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(5).toMillis();
        while (System.currentTimeMillis() < deadline && hintRepository.count() < expectedCount) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private AITask persistTaskGraph() {
        Environment environment = new Environment();
        environment.setTenantId(TENANT_ID);
        environment.setName("env");
        environment.setEnvironmentType(EnvironmentType.DEVELOPMENT);
        environnementRepository.save(environment);

        LLM llm = new LLM();
        llm.setTenantId(TENANT_ID);
        llm.setName("llm");
        llm.setFamily(LLMFamily.CHAT);
        llm.setModelName("gpt");
        llm.setMainPrompt("main");
        llm.setStructurePrompt("structure");
        llm.setLlmVendor(LLMVendor.OPENAI);
        llm.setApiKey("secret");
        llmRepository.save(llm);

        Application application = new Application();
        application.setTenantId(TENANT_ID);
        application.setName("app");
        application.setDefaultDBType(DBType.PGSQL);
        application.setDdlPrompt("ddl");
        application.setEventsPrompt("server");
        application.setDefaultLLM(llm);
        applicationRepository.save(application);

        Query query = new Query();
        query.setTenantId(TENANT_ID);
        query.setHashcode("hash");
        query.setSql("SELECT 1");
        query.setApplication(application);
        queryRepository.save(query);

        Run run = new Run();
        run.setTenantId(TENANT_ID);
        run.setDuration(10L);
        run.setCost(1.0);
        run.setPlan("PLAN");
        run.setStartTime(ZonedDateTime.now(ZoneOffset.UTC));
        run.setEndTime(run.getStartTime());
        run.setEnvironnement(environment);
        run.setQuery(query);
        runRepository.save(run);

        AITask task = new AITask();
        task.setTenantId(TENANT_ID);
        task.setProcessingStatus(ProcessingStatus.SCHEDULED);
        task.setRun(run);
        taskRepository.save(task);
        return task;
    }
}
