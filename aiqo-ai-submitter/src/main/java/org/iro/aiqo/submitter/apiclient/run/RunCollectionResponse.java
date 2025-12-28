package org.iro.aiqo.submitter.apiclient.run;

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
public class RunCollectionResponse {

    @JsonProperty("_embedded")
    private EmbeddedRuns embedded;

    private PageMetadata page;

    @JsonIgnore
    public List<Run> getItems() {
        if (embedded == null || embedded.runs == null) {
            return Collections.emptyList();
        }
        return embedded.runs;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbeddedRuns {

        @JsonProperty("runs")
        private List<Run> runs;

    }
}
