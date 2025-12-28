package org.iro.aiqo.collector.logparser.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {

    private Long endTime;
    private Long startTime;
    private String jobName;
    private String queryName;
    private String queryText;
    private String executionPlan;
    private Double durationMs;
    private Double startupCost;
    private Double cost;
    private Integer rows;
    private Buffers buffers;
    private Wal wal;
    private String hashcode;
}
