package org.iro.aiqo.submitter.apiclient.query;

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
public class QueryCollectionResponse {

    @JsonProperty("_embedded")
    private EmbeddedQueries embedded;

    private PageMetadata page;

    @JsonIgnore
    public List<Query> getItems() {
        if (embedded == null || embedded.queries == null) {
            return Collections.emptyList();
        }
        return embedded.queries;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbeddedQueries {

        @JsonProperty("queries")
        private List<Query> queries;

    }
}
