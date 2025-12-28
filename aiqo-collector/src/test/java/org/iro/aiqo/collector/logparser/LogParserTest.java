package org.iro.aiqo.collector.logparser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.iro.aiqo.collector.logparser.model.*;
import org.iro.aiqo.collector.logparser.parser.PlainTextLogFileParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogParserTest {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final List<String> SAMPLE_LINES = List.of(
            "2023-09-06 12:34:56.789 LOG:  plan: duration: 123.45 ms",
            "Query Text:",
            "-- JobName",
            "-- QueryName",
            "SELECT * FROM users WHERE id = 1;",
            "Seq Scan on users  (cost=0.00..1.01 rows=1 width=4) (actual rows=1 loops=1)",
            "  Buffers: shared hit=27378534 read=18805488 dirtied=8212390 written=77793, temp read=983279 written=983279",
            "  WAL: records=16045574 fpi=6001856 bytes=20460302613",
            "Settings: some_setting=on",
            "",
            "2023-09-06 13:00:00.000 LOG:  plan:",
            "Query Text:",
            "SELECT count(*) FROM orders;",
            "Aggregate  (cost=5.00..10.00 rows=1 width=8) (actual rows=1 loops=1)",
            "Settings: some_other_setting=off",
            "",
            "2023-09-06 14:00:00.000 LOG:  plan: duration: 10.5 ms",
            "Query Text:",
            "-- Job Insert",
            "-- Insert New User",
            "INSERT INTO users(name) VALUES ('foo');",
            "Insert on users  (cost=0.00..1.00 rows=0 width=0)",
            "  ->  Seq Scan on users  (cost=0.00..1.00 rows=5 width=0) (actual rows=5 loops=1)",
            "  Buffers: shared hit=1000 read=500",
            "Settings: apply=on"
    );

    @TempDir
    Path tempDir;
    private final SQLNormalizer sqlNormalizer = new SQLNormalizer();

    @Test
    void parsePlainLogAndResume() throws IOException {
        Path logFile = writePlainLog("auto_explain.log");
        PlainTextLogFileParser parser = new PlainTextLogFileParser(sqlNormalizer);

        ParseResult result = parser.parse(logFile, -1);
        assertEquals(SAMPLE_LINES.size() - 1, result.getLastLineProcessed());
        assertEquals(3, result.getEntries().size());

        LogEntry first = result.getEntries().get(0);
        long firstEnd = epochMillis("2023-09-06 12:34:56.789");
        assertEquals(firstEnd, first.getEndTime());
        assertEquals(firstEnd - Math.round(123.45), first.getStartTime());
        assertEquals("-- JobName", first.getJobName());
        assertEquals("-- QueryName", first.getQueryName());
        assertEquals("SELECT * FROM users WHERE id = 1;", first.getQueryText());
        assertTrue(first.getExecutionPlan().contains("Seq Scan on users"));
        assertTrue(first.getExecutionPlan().endsWith("Settings: some_setting=on"));
        assertEquals(123.45, first.getDurationMs(), 1e-6);
        assertEquals(0.00, first.getStartupCost(), 1e-6);
        assertEquals(1.01, first.getCost(), 1e-6);
        assertEquals(1, first.getRows());
        Buffers buffers = first.getBuffers();
        assertNotNull(buffers);
        assertEquals(27378534L, buffers.getSharedHit());
        assertEquals(18805488L, buffers.getSharedRead());
        assertEquals(8212390L, buffers.getSharedDirtied());
        assertEquals(77793L, buffers.getSharedWritten());
        assertEquals(983279L, buffers.getTempRead());
        assertEquals(983279L, buffers.getTempWritten());
        Wal wal = first.getWal();
        assertNotNull(wal);
        assertEquals(16045574L, wal.getRecords());
        assertEquals(6001856L, wal.getFpi());
        assertEquals(20460302613L, wal.getBytes());
        assertEquals(sqlNormalizer.normalizedHash(first.getQueryText()), first.getHashcode());

        LogEntry second = result.getEntries().get(1);
        long secondEnd = epochMillis("2023-09-06 13:00:00.000");
        assertEquals(secondEnd, second.getEndTime());
        assertNull(second.getStartTime());
        assertEquals("", second.getJobName());
        assertEquals("SELECT count(*) FROM orders;", second.getQueryName());
        assertEquals("SELECT count(*) FROM orders;", second.getQueryText());
        assertNull(second.getDurationMs());
        assertNull(second.getBuffers());
        assertNull(second.getWal());
        assertEquals(sqlNormalizer.normalizedHash(second.getQueryText()), second.getHashcode());

        LogEntry third = result.getEntries().get(2);
        long thirdEnd = epochMillis("2023-09-06 14:00:00.000");
        assertEquals(thirdEnd, third.getEndTime());
        assertEquals(thirdEnd - Math.round(10.5), third.getStartTime());
        assertEquals("-- Job Insert", third.getJobName());
        assertEquals("-- Insert New User", third.getQueryName());
        assertEquals("INSERT INTO users(name) VALUES ('foo');", third.getQueryText());
        assertEquals(10.5, third.getDurationMs(), 1e-6);
        assertEquals(0.00, third.getStartupCost(), 1e-6);
        assertEquals(1.00, third.getCost(), 1e-6);
        assertEquals(5, third.getRows());
        Buffers partialBuffers = third.getBuffers();
        assertNotNull(partialBuffers);
        assertEquals(1000L, partialBuffers.getSharedHit());
        assertEquals(500L, partialBuffers.getSharedRead());
        assertNull(partialBuffers.getSharedDirtied());
        assertNull(partialBuffers.getSharedWritten());
        assertNull(partialBuffers.getTempRead());
        assertNull(partialBuffers.getTempWritten());
        assertEquals(sqlNormalizer.normalizedHash(third.getQueryText()), third.getHashcode());

        ParseResult resumed = parser.parse(logFile, result.getLastLineProcessed());
        assertTrue(resumed.getEntries().isEmpty());
        assertEquals(result.getLastLineProcessed(), resumed.getLastLineProcessed());
    }

    @Test
    void parseGzipLog() throws IOException {
        Path gzFile = writeGzipLog("auto_explain.log.gz");
        PlainTextLogFileParser parser = new PlainTextLogFileParser(sqlNormalizer);

        ParseResult result = parser.parse(gzFile, -1);
        assertEquals(3, result.getEntries().size());
    }

    @Test
    void parseZipLog() throws IOException {
        Path zipFile = writeZipLog("auto_explain.zip", "logs/sample.log");
        PlainTextLogFileParser parser = new PlainTextLogFileParser(sqlNormalizer);

        ParseResult result = parser.parse(zipFile, -1);
        assertEquals(3, result.getEntries().size());
    }

    @Test
    void parseTimestampWithZoneAbbreviation() throws IOException {
        List<String> lines = List.of(
                "2025-06-08 03:56:11 CES LOG:  plan: duration: 5 ms",
                "Query Text:",
                "SELECT 1;",
                "Seq Scan on dual  (cost=0.00..1.00 rows=1 width=0)",
                "Settings: done"
        );
        Path file = tempDir.resolve("timezone.log");
        Files.writeString(file, String.join("\n", lines) + "\n");

        PlainTextLogFileParser parser = new PlainTextLogFileParser(sqlNormalizer);
        ParseResult result = parser.parse(file, -1);

        assertEquals(1, result.getEntries().size());
        LogEntry entry = result.getEntries().get(0);
        long expectedEnd = OffsetDateTime.of(2025, 6, 8, 3, 56, 11, 0, ZoneOffset.ofHours(2))
                .toInstant()
                .toEpochMilli();
        assertEquals(expectedEnd, entry.getEndTime());
        assertEquals(expectedEnd - 5, entry.getStartTime());
        assertEquals(5.0, entry.getDurationMs());
    }

    private Path writePlainLog(String fileName) throws IOException {
        Path path = tempDir.resolve(fileName);
        Files.writeString(path, String.join("\n", SAMPLE_LINES) + "\n");
        return path;
    }

    private Path writeGzipLog(String fileName) throws IOException {
        Path path = tempDir.resolve(fileName);
        try (OutputStream out = Files.newOutputStream(path);
             GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write((String.join("\n", SAMPLE_LINES) + "\n").getBytes(StandardCharsets.UTF_8));
        }
        return path;
    }

    private Path writeZipLog(String fileName, String entryName) throws IOException {
        Path path = tempDir.resolve(fileName);
        try (OutputStream out = Files.newOutputStream(path);
             ZipOutputStream zipOut = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            ZipEntry entry = new ZipEntry(entryName);
            zipOut.putNextEntry(entry);
            zipOut.write((String.join("\n", SAMPLE_LINES) + "\n").getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
        }
        return path;
    }

    private static long epochMillis(String timestamp) {
        return LocalDateTime.parse(timestamp, TIMESTAMP_FORMATTER)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }
}
