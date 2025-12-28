package org.iro.aiqo.collector.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.iro.aiqo.collector.AiqoCollectorApplication;
import org.iro.aiqo.collector.logparser.LogFileParser;
import org.iro.aiqo.collector.logparser.model.LogEntry;
import org.iro.aiqo.collector.logparser.model.ParseResult;
import org.iro.aiqo.collector.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = LocalFileCollectorIntegrationTest.TestApplication.class, properties = {
        "collector.application=test-app",
        "collector.environment=test-env",
        "collector.tenant=123",
        "spring.task.scheduling.enabled=false"
})
class LocalFileCollectorIntegrationTest {

    @TempDir
    Path tempDir;

    @Autowired
    private LocalFileCollector collector;

    @Autowired
    private LocalFileCollectorProperties properties;

    @MockBean
    private LogFileParser logFileParser;

    @MockBean
    private QueryService queryService;

    @BeforeEach
    void configureProperties() {
        properties.setDirectory(tempDir);
        properties.setFilePatterns(List.of("*.log"));
        properties.setScanInterval(Duration.ofMillis(100));
    }

    @Test
    void collectInvokesParserAndDelegatesEntriesToQueryService() throws IOException {
        String fileName = "collector-" + UUID.randomUUID() + ".log";
        Path logFile = tempDir.resolve(fileName);
        Files.writeString(logFile, "line1\nline2\nline3\n");
        Path stateFile = Paths.get("/tmp", fileName + ".lastindex");

        LogEntry firstEntry = new LogEntry();
        firstEntry.setHashcode("hash-1");
        LogEntry secondEntry = new LogEntry();
        secondEntry.setHashcode("hash-2");
        ParseResult parseResult = new ParseResult(List.of(firstEntry, secondEntry), 2);
        when(logFileParser.parse(any(Path.class), anyLong())).thenReturn(new ParseResult(List.of(), -1));
        when(logFileParser.parse(eq(logFile), eq(-1L))).thenReturn(parseResult);

        try {
            collector.collect();

            verify(logFileParser, times(1)).parse(eq(logFile), eq(-1L));
            ArgumentCaptor<LogEntry> entryCaptor = ArgumentCaptor.forClass(LogEntry.class);
            verify(queryService, times(2)).addQueryRun(entryCaptor.capture());
            assertThat(entryCaptor.getAllValues()).containsExactly(firstEntry, secondEntry);
            assertThat(Files.readString(stateFile)).isEqualTo("2");
        } finally {
            Files.deleteIfExists(stateFile);
        }
    }

    @SpringBootApplication(scanBasePackages = "org.iro.aiqo.collector")
    @ComponentScan(basePackages = "org.iro.aiqo.collector",
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AiqoCollectorApplication.class))
    static class TestApplication {
        // No scheduling enabled for integration tests
    }
}
