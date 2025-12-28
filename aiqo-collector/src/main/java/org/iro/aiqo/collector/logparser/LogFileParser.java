package org.iro.aiqo.collector.logparser;

import org.iro.aiqo.collector.logparser.model.ParseResult;

import java.io.IOException;
import java.nio.file.Path;

public interface LogFileParser extends  LogParser {
    ParseResult parse(Path file, long lastProcessedLine) throws IOException;
}
