package org.iro.aiqo.collector.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.iro.aiqo.collector.logparser.model.LogEntry;
import org.iro.aiqo.collector.logparser.model.ParseResult;
import org.iro.aiqo.collector.service.CollectorRunProducer;
import org.iro.aiqo.collector.service.QueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileCollectorTest {

    @TempDir
    Path tempDir;

    @Test
    void collectProcessesFileWhenNewLinesPresent() throws IOException {
        String fileName = "collector-" + UUID.randomUUID() + ".log";
        Path logFile = tempDir.resolve(fileName);
        Files.writeString(logFile, "line1\nline2\nline3\n");
        Path stateFile = Paths.get("/tmp", fileName + ".lastindex");

        LocalFileCollectorProperties properties = new LocalFileCollectorProperties();
        properties.setDirectory(tempDir);
        properties.setFilePatterns(List.of("*.log"));
        properties.setScanInterval(Duration.ofMillis(100));

        ParseResult parseResult = new ParseResult(
                List.of(new LogEntry(), new LogEntry()),
                2
        );

        StubLogFileParser parser = new StubLogFileParser();
        parser.setParseResult(parseResult);
        RecordingQueryService queryService = new RecordingQueryService();
        LocalFileCollector collector = new LocalFileCollector(properties, parser, queryService, "application", "tenant", "environment");

        try {
            collector.collect();
            assertEquals(1, parser.getInvocationCount());
            assertEquals(logFile, parser.getLastFile());
            assertEquals(-1L, parser.getLastOffset());
            assertTrue(Files.exists(stateFile));
            assertEquals("2", Files.readString(stateFile));
            assertEquals(2, queryService.getRecordedEntries().size());
            assertEquals(parseResult.getEntries(), queryService.getRecordedEntries());
        } finally {
            Files.deleteIfExists(stateFile);
        }
    }

    @Test
    void collectSkipsProcessingWhenNoNewLines() throws IOException {
        String fileName = "collector-" + UUID.randomUUID() + ".log";
        Path logFile = tempDir.resolve(fileName);
        Files.writeString(logFile, "line1\nline2\n");
        Path stateFile = Paths.get("/tmp", fileName + ".lastindex");
        Files.writeString(stateFile, "1");

        LocalFileCollectorProperties properties = new LocalFileCollectorProperties();
        properties.setDirectory(tempDir);
        properties.setFilePatterns(List.of("*.log"));
        properties.setScanInterval(Duration.ofMillis(100));

        StubLogFileParser parser = new StubLogFileParser();
        RecordingQueryService queryService = new RecordingQueryService();
        LocalFileCollector collector = new LocalFileCollector(properties, parser, queryService, "application", "tenant", "environment");

        try {
            collector.collect();
            assertEquals(0, parser.getInvocationCount());
            assertEquals("1", Files.readString(stateFile));
            assertTrue(queryService.getRecordedEntries().isEmpty());
        } finally {
            Files.deleteIfExists(stateFile);
        }
    }

    @Test
    void collectCreatesStateFileWhenMissingAndNoProcessingNeeded() throws IOException {
        String fileName = "collector-" + UUID.randomUUID() + ".log";
        Path logFile = tempDir.resolve(fileName);
        Files.writeString(logFile, "");
        Path stateFile = Paths.get("/tmp", fileName + ".lastindex");

        LocalFileCollectorProperties properties = new LocalFileCollectorProperties();
        properties.setDirectory(tempDir);
        properties.setFilePatterns(List.of("*.log"));
        properties.setScanInterval(Duration.ofMillis(100));

        StubLogFileParser parser = new StubLogFileParser();
        RecordingQueryService queryService = new RecordingQueryService();
        LocalFileCollector collector = new LocalFileCollector(properties, parser, queryService, "application", "tenant", "environment");

        try {
            collector.collect();
            assertTrue(Files.exists(stateFile));
            assertEquals("-1", Files.readString(stateFile));
            assertEquals(0, parser.getInvocationCount());
            assertTrue(queryService.getRecordedEntries().isEmpty());
        } finally {
            Files.deleteIfExists(stateFile);
        }
    }

    private static final class RecordingQueryService extends QueryService {

        private final List<LogEntry> recordedEntries = new ArrayList<>();

        RecordingQueryService() {
            super(new RecordingProducer(), "application", "environment", "1");
        }

        @Override
        public void addQueryRun(LogEntry logEntry) {
            recordedEntries.add(logEntry);
        }

        List<LogEntry> getRecordedEntries() {
            return recordedEntries;
        }

        private static final class RecordingProducer extends CollectorRunProducer {

            RecordingProducer() {
                super(null, null);
            }

            @Override
            public void send(org.iro.aiqo.collector.messaging.CollectorRunMessage message) {
                // no-op for test isolation
            }
        }
    }

    private static final class StubLogFileParser implements org.iro.aiqo.collector.logparser.LogFileParser {

        private int invocationCount;
        private Path lastFile;
        private long lastOffset;
        private ParseResult parseResult = new ParseResult(List.of(), -1);

        void setParseResult(ParseResult parseResult) {
            this.parseResult = parseResult;
        }

        int getInvocationCount() {
            return invocationCount;
        }

        Path getLastFile() {
            return lastFile;
        }

        long getLastOffset() {
            return lastOffset;
        }

        @Override
        public ParseResult parse(Path file, long lastProcessedLine) {
            invocationCount++;
            lastFile = file;
            lastOffset = lastProcessedLine;
            return parseResult;
        }
    }
}
