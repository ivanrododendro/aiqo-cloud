package org.iro.aiqo.collector.logparser.parser;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iro.aiqo.collector.logparser.LogFileParser;
import org.iro.aiqo.collector.logparser.model.Buffers;
import org.iro.aiqo.collector.logparser.model.LogEntry;
import org.iro.aiqo.collector.logparser.model.ParseResult;
import org.iro.aiqo.collector.logparser.model.Wal;
import org.iro.aiqo.collector.logparser.SQLNormalizer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlainTextLogFileParser implements LogFileParser {

    private static final Pattern DURATION_PATTERN = Pattern.compile("duration:\\s+(\\d+(?:\\.\\d*)?)\\s+ms");
    private static final Pattern COST_PATTERN = Pattern.compile("cost=(\\d+(?:\\.\\d+)?)\\.\\.(\\d+(?:\\.\\d+)?)");
    private static final Pattern ACTUAL_ROWS_PATTERN = Pattern.compile("actual rows=(\\d+)");
    private static final Pattern ROWS_PATTERN = Pattern.compile("rows=(\\d+)");
    private static final Pattern BUFFERS_PATTERN = Pattern.compile(
            "Buffers:\\s+shared\\s+hit=(\\d+)(?:\\s+read=(\\d+))?(?:\\s+dirtied=(\\d+))?(?:\\s+written=(\\d+))?"
                    + "(?:,\\s*temp\\s+read=(\\d+))?(?:\\s+written=(\\d+))?"
    );
    private static final Pattern WAL_PATTERN = Pattern.compile(
            "WAL:\\s+records=(\\d+)(?:\\s+fpi=(\\d+))?(?:\\s+bytes=(\\d+))?"
    );
    private static final DateTimeFormatter BASE_TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart().appendPattern(".SSS").optionalEnd()
            .toFormatter(Locale.US);
    private static final ZoneOffset TIMESTAMP_ZONE = ZoneOffset.UTC;
    private static final DateTimeFormatter ZONED_TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(BASE_TIMESTAMP_FORMATTER)
            .appendLiteral(' ')
            .appendZoneText(java.time.format.TextStyle.SHORT)
            .toFormatter(Locale.US);
    private static final Map<String, ZoneOffset> ZONE_ABBREVIATIONS = Map.of(
            "UTC", ZoneOffset.UTC,
            "GMT", ZoneOffset.UTC,
            "CET", ZoneOffset.ofHours(1),
            "CEST", ZoneOffset.ofHours(2),
            "CES", ZoneOffset.ofHours(2)
    );
    private static final Pattern TIMESTAMP_PREFIX_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?)(?:\\s+([A-Za-z]{2,10}))?"
    );

    private final SQLNormalizer sqlNormalizer;

    public ParseResult parse(Path file, long lastProcessedLine) throws IOException {
        List<LogEntry> entries = new ArrayList<>();
        long lineIndex = -1;
        long effectiveLastProcessed = lastProcessedLine;

        try (LineReader reader = createLineReader(file)) {
            List<String> currentEntry = new ArrayList<>();
            boolean collecting = false;

            String line;
            while ((line = reader.readLine()) != null) {
                lineIndex++;

                if (lineIndex <= effectiveLastProcessed) {
                    continue;
                }

                if (!collecting) {
                    if (line.contains("plan:")) {
                        collecting = true;
                        currentEntry = new ArrayList<>();
                        currentEntry.add(line);
                    }
                } else {
                    currentEntry.add(line);
                }

                if (collecting && line.trim().startsWith("Settings:")) {
                    // Each block spans from the first "plan:" line to the first "Settings:" line included.
                    parseEntry(currentEntry).ifPresent(entries::add);
                    collecting = false;
                    currentEntry = new ArrayList<>();
                }
            }

            if (collecting && !currentEntry.isEmpty()) {
                parseEntry(currentEntry).ifPresent(entries::add);
            }
        }

        return new ParseResult(entries, lineIndex);
    }

    private Optional<LogEntry> parseEntry(List<String> entryLines) {
        if (entryLines.isEmpty()) {
            return Optional.empty();
        }

        String entryText = String.join("\n", entryLines);
        String rawTimestamp = extractTimestamp(entryLines.get(0));
        Long endTime = parseTimestamp(rawTimestamp);

        Double durationMs = parseDouble(findFirstMatch(DURATION_PATTERN, entryLines.get(0)));
        Long startTime = computeStartTime(endTime, durationMs);

        int queryTextIndex = findQueryTextIndex(entryLines);
        if (queryTextIndex < 0) {
            logSkipping("Missing 'Query Text:' marker", entryLines);
            return Optional.empty();
        }
        if (queryTextIndex + 1 >= entryLines.size()) {
            logSkipping("No content after 'Query Text:'", entryLines);
            return Optional.empty();
        }

        int planStartIndex = findPlanStart(entryLines, queryTextIndex + 1);
        if (planStartIndex < 0) {
            logSkipping("Missing plan line with 'cost='", entryLines);
            return Optional.empty();
        }

        String jobName;
        String queryName;
        int queryContentStart = queryTextIndex + 1;
        List<String> afterQuery = entryLines.subList(queryContentStart, planStartIndex);
        if (afterQuery.size() >= 2 && isDashLine(afterQuery.get(0)) && isDashLine(afterQuery.get(1))) {
            jobName = normalizeLabel(afterQuery.get(0));
            queryName = normalizeLabel(afterQuery.get(1));
            queryContentStart += 2;
        } else {
            jobName = "";
            queryName = collapseToSingleLine(afterQuery);
        }

        if (queryContentStart > planStartIndex) {
            queryContentStart = planStartIndex;
        }

        List<String> queryLines = entryLines.subList(queryContentStart, planStartIndex);
        String queryText = joinAndTrim(queryLines);
        String executionPlan = joinAndTrim(entryLines.subList(planStartIndex, entryLines.size()));
        String normalizedHash = sqlNormalizer.normalizedHash(queryText);

        Double startupCost = null;
        Double totalCost = null;
        String planFirstLine = entryLines.get(planStartIndex);
        Matcher costMatcher = COST_PATTERN.matcher(planFirstLine);
        if (costMatcher.find()) {
            startupCost = parseDouble(costMatcher.group(1));
            totalCost = parseDouble(costMatcher.group(2));
        }

        Integer rows = extractRows(entryLines.subList(planStartIndex, entryLines.size()));

        Buffers buffers = parseBuffers(entryText);
        Wal wal = parseWal(entryText);

        LogEntry entry = new LogEntry(
                endTime,
                startTime,
                normalizeLabel(jobName),
                normalizeLabel(queryName),
                queryText,
                executionPlan,
                durationMs,
                startupCost,
                totalCost,
                rows,
                buffers,
                wal,
                normalizedHash
        );

        return Optional.of(entry);
    }

    private Buffers parseBuffers(String entryText) {
        Matcher matcher = BUFFERS_PATTERN.matcher(entryText);
        if (!matcher.find()) {
            return null;
        }
        return new Buffers(
                parseLong(matcher.group(1)),
                parseLong(matcher.group(2)),
                parseLong(matcher.group(3)),
                parseLong(matcher.group(4)),
                parseLong(matcher.group(5)),
                parseLong(matcher.group(6))
        );
    }

    private Wal parseWal(String entryText) {
        Matcher matcher = WAL_PATTERN.matcher(entryText);
        if (!matcher.find()) {
            return null;
        }
        return new Wal(
                parseLong(matcher.group(1)),
                parseLong(matcher.group(2)),
                parseLong(matcher.group(3))
        );
    }

    private Integer extractRows(List<String> planLines) {
        if (planLines.isEmpty()) {
            return null;
        }
        String firstLine = planLines.get(0);

        Integer rows = parseInt(findFirstMatch(ACTUAL_ROWS_PATTERN, firstLine));
        if (rows == null) {
            rows = parseInt(findFirstMatch(ROWS_PATTERN, firstLine));
        }

        if ((rows == null || rows == 0) && containsInsertOrUpdate(firstLine)) {
            for (int i = 1; i < planLines.size(); i++) {
                String candidate = findFirstMatch(ACTUAL_ROWS_PATTERN, planLines.get(i));
                Integer value = parseInt(candidate);
                if (value != null && value > 0) {
                    rows = value;
                    break;
                }
            }
        }

        return rows;
    }

    private boolean containsInsertOrUpdate(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("insert") || lower.contains("update");
    }

    private String joinAndTrim(List<String> lines) {
        if (lines.isEmpty()) {
            return "";
        }
        return String.join("\n", lines).trim();
    }

    private String collapseToSingleLine(List<String> lines) {
        if (lines.isEmpty()) {
            return "";
        }
        String joined = String.join(" ", lines)
                .replace('\t', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ');
        return joined.trim().replaceAll("\\s+", " ");
    }

    private boolean isDashLine(String line) {
        return line.trim().startsWith("--");
    }

    private String normalizeLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replace("\r", "").trim();
    }

    private void logSkipping(String reason, List<String> entryLines) {
        log.warn("{} - skipping entry starting with: {}", reason, entryLines.get(0));
    }

    private String findFirstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Double parseDouble(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int findQueryTextIndex(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("Query Text:")) {
                return i;
            }
        }
        return -1;
    }

    private int findPlanStart(List<String> lines, int startIndex) {
        for (int i = startIndex; i < lines.size(); i++) {
            if (lines.get(i).contains("cost=")) {
                return i;
            }
        }
        return -1;
    }

    private LineReader createLineReader(Path file) throws IOException {
        String filename = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".gz")) {
            InputStream in = new GZIPInputStream(Files.newInputStream(file));
            return new InputStreamLineReader(in);
        }
        if (filename.endsWith(".zip")) {
            return new ZipLineReader(file);
        }
        InputStream in = Files.newInputStream(file);
        return new InputStreamLineReader(in);
    }

    private CharsetDecoder newDecoder() {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    private String extractTimestamp(String line) {
        if (line == null) {
            return null;
        }
        Matcher matcher = TIMESTAMP_PREFIX_PATTERN.matcher(line.trim());
        if (matcher.find()) {
            String base = matcher.group(1);
            String zoneCandidate = matcher.group(2);
            if (zoneCandidate != null && isZoneToken(zoneCandidate)) {
                return base + " " + zoneCandidate;
            }
            return base;
        }
        return null;
    }

    private Long parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return null;
        }
        try {
            return parseWithZone(timestamp);
        } catch (DateTimeParseException ex) {
            log.warn("Unable to parse timestamp '{}'; leaving unset. Cause: {}", timestamp, ex.getMessage());
            return null;
        }
    }

    private boolean isZoneToken(String token) {
        if (ZONE_ABBREVIATIONS.containsKey(token)) {
            return true;
        }
        try {
            java.time.ZoneId.of(token);
            return true;
        } catch (DateTimeException ex) {
            return false;
        }
    }

    private long parseWithZone(String timestamp) {
        String trimmed = timestamp.trim();
        try {
            LocalDateTime dateTime = LocalDateTime.parse(trimmed, BASE_TIMESTAMP_FORMATTER);
            return dateTime.toInstant(TIMESTAMP_ZONE).toEpochMilli();
        } catch (DateTimeParseException ignored) {
            int lastSpace = trimmed.lastIndexOf(' ');
            if (lastSpace > 0) {
                String zoneToken = trimmed.substring(lastSpace + 1);
                ZoneOffset offset = ZONE_ABBREVIATIONS.get(zoneToken);
                if (offset != null) {
                    String withoutZone = trimmed.substring(0, lastSpace);
                    LocalDateTime dateTime = LocalDateTime.parse(withoutZone, BASE_TIMESTAMP_FORMATTER);
                    return dateTime.toInstant(offset).toEpochMilli();
                }
            }
            OffsetDateTime zoned = OffsetDateTime.parse(trimmed, ZONED_TIMESTAMP_FORMATTER);
            return zoned.toInstant().toEpochMilli();
        }
    }

    private Long computeStartTime(Long endTime, Double durationMs) {
        if (endTime == null || durationMs == null) {
            return null;
        }
        long duration = Math.round(durationMs);
        return endTime - duration;
    }

    private interface LineReader extends Closeable {
        String readLine() throws IOException;
    }

    private class InputStreamLineReader implements LineReader {

        private final InputStream inputStream;
        private final BufferedReader reader;

        InputStreamLineReader(InputStream inputStream) {
            this.inputStream = inputStream;
            this.reader = new BufferedReader(new InputStreamReader(inputStream, newDecoder()));
        }

        @Override
        public String readLine() throws IOException {
            return reader.readLine();
        }

        @Override
        public void close() throws IOException {
            reader.close();
            inputStream.close();
        }
    }

    private class ZipLineReader implements LineReader {

        private final ZipFile zipFile;
        private final java.util.Enumeration<? extends ZipEntry> entries;
        private BufferedReader currentReader;

        ZipLineReader(Path path) throws IOException {
            this.zipFile = new ZipFile(path.toFile(), StandardCharsets.UTF_8);
            this.entries = zipFile.entries();
        }

        @Override
        public String readLine() throws IOException {
            while (true) {
                if (currentReader == null) {
                    if (!advanceEntry()) {
                        return null;
                    }
                }
                String line = currentReader.readLine();
                if (line != null) {
                    return line;
                }
                closeCurrentReader();
            }
        }

        @Override
        public void close() throws IOException {
            closeCurrentReader();
            zipFile.close();
        }

        private boolean advanceEntry() throws IOException {
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                InputStream entryStream = zipFile.getInputStream(entry);
                currentReader = new BufferedReader(new InputStreamReader(entryStream, newDecoder()));
                return true;
            }
            return false;
        }

        private void closeCurrentReader() throws IOException {
            if (currentReader != null) {
                currentReader.close();
                currentReader = null;
            }
        }
    }
}
