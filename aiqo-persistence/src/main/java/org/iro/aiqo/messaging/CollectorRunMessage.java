package org.iro.aiqo.messaging;

import java.time.OffsetDateTime;

public record CollectorRunMessage(
        Integer tenantId,
        String applicationName,
        String environmentName,
        String hashcode,
        String sql,
        String queryName,
        String logFilename,
        String originalRun,
        String notes,
        Long duration,
        Double cost,
        Double startupCost,
        Long rows,
        Long buffersSharedHit,
        Long buffersSharedRead,
        Long buffersSharedDirtied,
        Long buffersSharedWritten,
        Long buffersTempRead,
        Long buffersTempWritten,
        Long walRecords,
        Long walFpi,
        Long walBytes,
        String plan,
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {
}
