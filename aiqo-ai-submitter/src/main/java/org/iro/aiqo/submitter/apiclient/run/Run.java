package org.iro.aiqo.submitter.apiclient.run;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.iro.aiqo.submitter.apiclient.query.Query;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Run {

    private Long id;
    private String userCreated;
    private OffsetDateTime createdAt;
    private String userUpdated;
    private OffsetDateTime updatedAt;
    private Integer tenantId;
    private Long duration;
    private Double cost;
    private Double startupCost;
    private Long rows;
    private Long buffersSharedHit;
    private Long buffersSharedRead;
    private Long buffersSharedDirtied;
    private Long buffersSharedWritten;
    private Long buffersTempRead;
    private Long buffersTempWritten;
    private Long walRecords;
    private Long walFpi;
    private Long walBytes;
    private String plan;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Environment environnement;
    private Query query;

}
