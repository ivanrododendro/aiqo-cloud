package org.iro.aiqo.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.iro.aiqo.messaging.CollectorRunMessage;
import org.iro.aiqo.model.AITask;
import org.iro.aiqo.model.Application;
import org.iro.aiqo.model.Environment;
import org.iro.aiqo.model.ProcessingStatus;
import org.iro.aiqo.model.Query;
import org.iro.aiqo.model.Run;
import org.iro.aiqo.repository.AITaskRepository;
import org.iro.aiqo.repository.ApplicationRepository;
import org.iro.aiqo.repository.EnvironnementRepository;
import org.iro.aiqo.repository.QueryRepository;
import org.iro.aiqo.repository.RunRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorRunListener {

    private final ApplicationRepository applicationRepository;
    private final EnvironnementRepository environnementRepository;
    private final QueryRepository queryRepository;
    private final RunRepository runRepository;
    private final AITaskRepository aiTaskRepository;

    @KafkaListener(
            topics = "#{@kafkaTopicsProperties.collectorTopic}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = "spring.json.value.default.type=org.iro.aiqo.messaging.CollectorRunMessage"
    )
    @Transactional
    public void onCollectorRun(CollectorRunMessage message) {
        Objects.requireNonNull(message, "Collector message must not be null");
        log.info("Received collector run for hash {} tenant {}", message.hashcode(), message.tenantId());

        Application application = applicationRepository.findByTenantIdAndName(message.tenantId(), message.applicationName())
                .orElseThrow(() -> new IllegalStateException("Application not found: " + message.applicationName()));
        Environment environment = environnementRepository.findByName(message.environmentName())
                .orElseThrow(() -> new IllegalStateException("Environment not found: " + message.environmentName()));

        Query query = queryRepository.findByTenantIdAndHashcode(message.tenantId(), message.hashcode())
                .orElseGet(() -> createQuery(message, application));

        Run run = createRun(message, environment, query);
        createAiTask(message, run);
    }

    private Query createQuery(CollectorRunMessage message, Application application) {
        Query query = new Query();
        query.setTenantId(message.tenantId());
        query.setHashcode(message.hashcode());
        query.setSql(message.sql());
        query.setName(truncate(message.queryName(), 255));
        query.setJobName(message.logFilename());
        query.setComments(message.notes());
        query.setNotes(message.notes());
        query.setLogFilename(message.logFilename());
        query.setApplication(application);
        return queryRepository.save(query);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private Run createRun(CollectorRunMessage message, Environment environment, Query query) {
        Run run = new Run();
        run.setTenantId(message.tenantId());
        run.setDuration(requireNonNull(message.duration(), "duration"));
        run.setCost(requireNonNull(message.cost(), "cost"));
        run.setStartupCost(message.startupCost());
        run.setRows(message.rows());
        run.setBuffersSharedHit(message.buffersSharedHit());
        run.setBuffersSharedRead(message.buffersSharedRead());
        run.setBuffersSharedDirtied(message.buffersSharedDirtied());
        run.setBuffersSharedWritten(message.buffersSharedWritten());
        run.setBuffersTempRead(message.buffersTempRead());
        run.setBuffersTempWritten(message.buffersTempWritten());
        run.setWalRecords(message.walRecords());
        run.setWalFpi(message.walFpi());
        run.setWalBytes(message.walBytes());
        run.setPlan(requireNonNull(message.plan(), "plan"));
        run.setStartTime(requireZoned(message.startTime(), "startTime"));
        run.setEndTime(requireZoned(message.endTime(), "endTime"));
        run.setEnvironnement(environment);
        run.setQuery(query);
        return runRepository.save(run);
    }

    private void createAiTask(CollectorRunMessage message, Run run) {
        AITask task = new AITask();
        task.setTenantId(message.tenantId());
        task.setProcessingStatus(ProcessingStatus.SCHEDULED);
        task.setRun(run);
        aiTaskRepository.save(task);
    }

    private ZonedDateTime requireZoned(java.time.OffsetDateTime time, String field) {
        Objects.requireNonNull(time, field + " is required");
        return time.atZoneSameInstant(ZoneOffset.UTC);
    }

    private <T> T requireNonNull(T value, String field) {
        return Objects.requireNonNull(value, field + " is required");
    }
}
