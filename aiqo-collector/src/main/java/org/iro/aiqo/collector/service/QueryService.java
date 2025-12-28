package org.iro.aiqo.collector.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

import org.iro.aiqo.collector.messaging.CollectorRunMessage;
import org.iro.aiqo.collector.logparser.model.Buffers;
import org.iro.aiqo.collector.logparser.model.LogEntry;
import org.iro.aiqo.collector.logparser.model.Wal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QueryService {

    private final CollectorRunProducer collectorRunProducer;
    private final String application;
    private final String environment;
    private final int tenantId;

    public QueryService(
            CollectorRunProducer collectorRunProducer,
            @Value("${collector.application}") String application,
            @Value("${collector.environment}") String environment,
            @Value("${collector.tenant}") String tenant
    ) {
        this.collectorRunProducer = Objects.requireNonNull(collectorRunProducer, "collectorRunProducer must not be null");
        this.application = requireNonBlank(application, "collector.application");
        this.environment = requireNonBlank(environment, "collector.environment");
        this.tenantId = (int) parseTenantId(requireNonBlank(tenant, "collector.tenant"));
    }

    public void addQueryRun(LogEntry logEntry) {
        Objects.requireNonNull(logEntry, "logEntry must not be null");
        String hashcode = requireNonBlank(logEntry.getHashcode(), "logEntry.hashcode");
        String sql = requireNonBlank(logEntry.getQueryText(), "logEntry.queryText");
        String plan = requireNonBlank(logEntry.getExecutionPlan(), "logEntry.executionPlan");

        log.info("Publishing collector run for hash {} and tenant {}", hashcode, tenantId);

        CollectorRunMessage message = buildMessage(logEntry, hashcode, sql, plan);
        collectorRunProducer.send(message);
    }

    private CollectorRunMessage buildMessage(LogEntry logEntry, String hashcode, String sql, String plan) {
        Buffers buffers = logEntry.getBuffers();
        Wal wal = logEntry.getWal();
        return new CollectorRunMessage(
                tenantId,
                application,
                environment,
                hashcode,
                sql,
                logEntry.getQueryName(),
                logEntry.getJobName(),
                logEntry.getJobName(),
                logEntry.getQueryName(),
                toLong(logEntry.getDurationMs()),
                logEntry.getCost(),
                logEntry.getStartupCost(),
                toLong(logEntry.getRows()),
                buffers != null ? buffers.getSharedHit() : null,
                buffers != null ? buffers.getSharedRead() : null,
                buffers != null ? buffers.getSharedDirtied() : null,
                buffers != null ? buffers.getSharedWritten() : null,
                buffers != null ? buffers.getTempRead() : null,
                buffers != null ? buffers.getTempWritten() : null,
                wal != null ? wal.getRecords() : null,
                wal != null ? wal.getFpi() : null,
                wal != null ? wal.getBytes() : null,
                plan,
                toOffsetDateTime(logEntry.getStartTime()),
                toOffsetDateTime(logEntry.getEndTime())
        );
    }

    private OffsetDateTime toOffsetDateTime(Long epochMillis) {
        if (epochMillis == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }

    private Long toLong(Number value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double doubleValue) {
            return Math.round(doubleValue);
        }
        return value.longValue();
    }

    private String requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(propertyName + " must be provided");
        }
        return value;
    }

    private long parseTenantId(String tenantValue) {
        try {
            return Long.parseLong(tenantValue);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("collector.tenant must be a numeric identifier", ex);
        }
    }
}
