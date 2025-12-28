package org.iro.aiqo.submitter.apiclient.application;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.iro.aiqo.submitter.apiclient.task.domain.LLM;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Application {

    private Long id;
    private String userCreated;
    private OffsetDateTime createdAt;
    private String userUpdated;
    private OffsetDateTime updatedAt;
    private Integer tenantId;
    private String name;
    private DatabaseType defaultDBType;
    private String ddlPrompt;
    private String applicationPrompt;
    private String configPrompt;
    private String eventsPrompt;
    private LLM defaultLLM;
    private Boolean scheduleForLowCost;
    private Boolean limitToFreeTier;

}
