package org.iro.aiqo.collector.logparser.model;

import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
public class ParseResult {

    private final List<LogEntry> entries;
    private final long lastLineProcessed;

    public ParseResult(List<LogEntry> entries, long lastLineProcessed) {
        this.entries = List.copyOf(entries);
        this.lastLineProcessed = lastLineProcessed;
    }

    public List<LogEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
