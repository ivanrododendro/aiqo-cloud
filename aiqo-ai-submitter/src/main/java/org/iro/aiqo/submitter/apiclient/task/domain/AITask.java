package org.iro.aiqo.submitter.apiclient.task.domain;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.iro.aiqo.submitter.apiclient.run.Run;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AITask {

    private Long id;
    private String userCreated;
    private OffsetDateTime createdAt;
    private String userUpdated;
    private OffsetDateTime updatedAt;
    private Integer tenantId;
    private ProcessingStatus processingStatus;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Long inputTokens;
    private Long outputTokens;
    private Long estimatedCost;
    private OffsetDateTime lockedAt;
    private CurrencyInfo currency;
    private Run run;
    @JsonProperty("llmserved")
    private LLM llmServed;

}
