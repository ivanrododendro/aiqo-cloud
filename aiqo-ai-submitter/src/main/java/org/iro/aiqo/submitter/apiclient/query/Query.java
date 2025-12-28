package org.iro.aiqo.submitter.apiclient.query;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.iro.aiqo.submitter.apiclient.application.Application;
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Query {

    private Long id;
    private String userCreated;
    private OffsetDateTime createdAt;
    private String userUpdated;
    private OffsetDateTime updatedAt;
    private Integer tenantId;
    private String hashcode;
    private String sql;
    private String name;
    private String jobName;
    private String comments;
    private Boolean fullyOptimized;
    private String notes;
    private String logFilename;
    private Application application;
    private Query originalQuery;

}
