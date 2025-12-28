package org.iro.aiqo.submitter.apiclient.hint;

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
public class AIHintCollectionResponse {

    @JsonProperty("_embedded")
    private EmbeddedAIHints embedded;

    private PageMetadata page;

    @JsonIgnore
    public List<AIHint> getItems() {
        if (embedded == null || embedded.hints == null) {
            return Collections.emptyList();
        }
        return embedded.hints;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbeddedAIHints {

        @JsonProperty("aIHints")
        private List<AIHint> hints;

    }
}
