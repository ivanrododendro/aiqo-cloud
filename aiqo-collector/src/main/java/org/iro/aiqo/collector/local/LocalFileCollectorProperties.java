package org.iro.aiqo.collector.local;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component("localFileCollectorProperties")
@ConfigurationProperties(prefix = "collector.local")
public class LocalFileCollectorProperties {

    /**
     * Directory that will be scanned for log files.
     */
    private Path directory;

    /**
     * List of glob patterns used to match log files in the scanned directory.
     */
    private List<String> filePatterns;

    /**
     * Interval between consecutive directory scans.
     */
    private Duration scanInterval = Duration.ofSeconds(30);
}
