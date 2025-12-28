package org.iro.aiqo.collector.local;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.extern.slf4j.Slf4j;
import org.iro.aiqo.collector.logparser.LogFileParser;
import org.iro.aiqo.collector.logparser.model.ParseResult;
import org.iro.aiqo.collector.service.QueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LocalFileCollector {

    private static final Path STATE_DIRECTORY = Paths.get("/tmp");
    private static final String STATE_SUFFIX = ".lastindex";

    private final LocalFileCollectorProperties properties;
    private final LogFileParser logFileParser;
    private final QueryService queryService;
    private final String application;
    private final String tenant;
    private final String environment;

    public LocalFileCollector(LocalFileCollectorProperties properties,
                              LogFileParser logFileParser,
                              QueryService queryService,
                              @Value("${collector.application}") String application,
                              @Value("${collector.tenant}") String tenant,
                              @Value("${collector.environment}") String environment) {
        this.properties = properties;
        this.logFileParser = logFileParser;
        this.queryService = queryService;
        this.application = application;
        this.tenant = tenant;
        this.environment = environment;
    }

    @Scheduled(fixedDelayString = "#{@localFileCollectorProperties.scanInterval.toMillis()}")
    public void collect() {
        Path directory = properties.getDirectory();
        if (directory == null) {
            log.debug("Local collector directory not configured; skipping scan.");
            return;
        }
        if (!Files.isDirectory(directory)) {
            log.warn("Configured directory {} is not a directory; skipping scan.", directory);
            return;
        }

        List<String> patterns = properties.getFilePatterns();
        if (patterns == null || patterns.isEmpty()) {
            log.debug("No file patterns configured; skipping scan.");
            return;
        }

        List<PathMatcher> matchers = buildMatchers(patterns);
        if (matchers.isEmpty()) {
            log.debug("No valid file patterns resolved; skipping scan.");
            return;
        }

        try (var files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(file -> matchesAny(file.getFileName(), matchers))
                    .forEach(this::processFileSafely);
        } catch (IOException e) {
            log.error("Failed to scan directory {}", directory, e);
        }
    }

    private List<PathMatcher> buildMatchers(List<String> patterns) {
        List<PathMatcher> matchers = new ArrayList<>();
        for (String pattern : patterns) {
            createMatcher(pattern).ifPresent(matchers::add);
        }
        return matchers;
    }

    private Optional<PathMatcher> createMatcher(String pattern) {
        if (pattern == null) {
            return Optional.empty();
        }
        String trimmed = pattern.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(FileSystems.getDefault().getPathMatcher("glob:" + trimmed));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid file pattern '{}'; ignoring.", trimmed, ex);
            return Optional.empty();
        }
    }

    private boolean matchesAny(Path fileName, List<PathMatcher> matchers) {
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(fileName)) {
                return true;
            }
        }
        return false;
    }

    private void processFileSafely(Path file) {
        try {
            processFile(file);
        } catch (IOException e) {
            log.error("Failed to process file {}", file, e);
        }
    }

    private void processFile(Path file) throws IOException {
        Path stateFile = STATE_DIRECTORY.resolve(file.getFileName().toString() + STATE_SUFFIX);

        long lastProcessed = readLastProcessed(stateFile);
        long totalLines = countLines(file);

        if (lastProcessed >= totalLines - 1) {
            log.debug("No new content for file {} (last index: {}, total lines: {}).", file, lastProcessed, totalLines);
            return;
        }

        ParseResult result = logFileParser.parse(file, lastProcessed);
        result.getEntries().forEach(queryService::addQueryRun);
        log.info("Parsed {} entries from {} for application {} tenant {} environment {}",
                result.getEntries().size(), file, application, tenant, environment);
        writeLastProcessed(stateFile, result.getLastLineProcessed());
    }

    private long readLastProcessed(Path stateFile) {
        if (!Files.exists(stateFile)) {
            try {
                Path parent = stateFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(stateFile, "-1");
            } catch (IOException e) {
                log.warn("Could not initialize state file {}; starting from beginning.", stateFile, e);
            }
            return -1L;
        }
        try {
            String value = Files.readString(stateFile).trim();
            if (value.isEmpty()) {
                return -1L;
            }
            return Long.parseLong(value);
        } catch (IOException | NumberFormatException e) {
            log.warn("Could not read last processed index from {}; starting from beginning.", stateFile, e);
            return -1L;
        }
    }

    private void writeLastProcessed(Path stateFile, long lastProcessed) throws IOException {
        Files.createDirectories(stateFile.getParent());
        Files.writeString(stateFile, Long.toString(lastProcessed));
    }

    private long countLines(Path file) throws IOException {
        String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".gz")) {
            try (InputStream raw = Files.newInputStream(file);
                 GZIPInputStream gzip = new GZIPInputStream(raw);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(gzip, newDecoder()))) {
                return countLines(reader);
            }
        }
        if (fileName.endsWith(".zip")) {
            try (ZipFile zipFile = new ZipFile(file.toFile(), StandardCharsets.UTF_8)) {
                long total = 0;
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        continue;
                    }
                    try (InputStream entryStream = zipFile.getInputStream(entry);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(entryStream, newDecoder()))) {
                        total += countLines(reader);
                    }
                }
                return total;
            }
        }
        try (InputStream input = Files.newInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, newDecoder()))) {
            return countLines(reader);
        }
    }

    private long countLines(BufferedReader reader) throws IOException {
        long count = 0;
        while (reader.readLine() != null) {
            count++;
        }
        return count;
    }

    private CharsetDecoder newDecoder() {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }
}
