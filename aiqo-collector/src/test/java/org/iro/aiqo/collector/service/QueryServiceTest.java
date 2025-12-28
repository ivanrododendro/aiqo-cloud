package org.iro.aiqo.collector.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.iro.aiqo.collector.messaging.CollectorRunMessage;
import org.iro.aiqo.collector.logparser.model.Buffers;
import org.iro.aiqo.collector.logparser.model.LogEntry;
import org.iro.aiqo.collector.logparser.model.Wal;
import org.junit.jupiter.api.Test;

class QueryServiceTest {

    @Test
    void addQueryRunPublishesCollectorMessage() {
        RecordingProducer producer = new RecordingProducer();
        QueryService service = new QueryService(producer, "app", "DEV", "9");

        Buffers buffers = new Buffers(1L, 2L, 3L, 4L, 5L, 6L);
        Wal wal = new Wal(7L, 8L, 9L);
        LogEntry entry = new LogEntry(
                1_700_000_000_000L,
                1_699_999_998_800L,
                "job",
                "query",
                "select 1",
                "plan",
                1200.0,
                0.1,
                1.0,
                5,
                buffers,
                wal,
                "hash"
        );

        service.addQueryRun(entry);

        CollectorRunMessage sent = producer.lastMessage;
        assertThat(sent).isNotNull();
        assertThat(sent.tenantId()).isEqualTo(9);
        assertThat(sent.applicationName()).isEqualTo("app");
        assertThat(sent.environmentName()).isEqualTo("DEV");
        assertThat(sent.hashcode()).isEqualTo("hash");
        assertThat(sent.sql()).isEqualTo("select 1");
        assertThat(sent.plan()).isEqualTo("plan");
        assertThat(sent.duration()).isEqualTo(1200L);
        assertThat(sent.startTime()).isEqualTo(OffsetDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(1_699_999_998_800L),
                ZoneOffset.UTC));
    }

    private static class RecordingProducer extends CollectorRunProducer {

        private CollectorRunMessage lastMessage;

        RecordingProducer() {
            super(null, null);
        }

        @Override
        public void send(CollectorRunMessage message) {
            this.lastMessage = message;
        }
    }
}
