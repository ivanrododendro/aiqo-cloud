package org.iro.aiqo.submitter.apiclient.task.domain;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LLM {

    private Long id;
    private String userCreated;
    private OffsetDateTime createdAt;
    private String userUpdated;
    private OffsetDateTime updatedAt;
    private Integer tenantId;
    private String name;
    private ModelFamily family;
    private String provider;
    private String modelName;
    private Integer maxInputTokens;
    private Integer maxOutputTokens;
    private String mainPrompt;
    private String ddlPrompt;
    private String serverHintsPrompt;
    private String structurePrompt;
    private Integer freeTierTPM;
    private String apiUrl;
    private String apiKey;

}
