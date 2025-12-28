package org.iro.aiqo.submitter.apiclient.application;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.iro.aiqo.submitter.apiclient.common.PageMetadata;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationCollectionResponse {

    @JsonProperty("_embedded")
    private EmbeddedApplications embedded;

    private PageMetadata page;

    @JsonIgnore
    public List<Application> getItems() {
        if (embedded == null || embedded.applications == null) {
            return Collections.emptyList();
        }
        return embedded.applications;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbeddedApplications {

        @JsonProperty("applications")
        private List<Application> applications;

    }
}
