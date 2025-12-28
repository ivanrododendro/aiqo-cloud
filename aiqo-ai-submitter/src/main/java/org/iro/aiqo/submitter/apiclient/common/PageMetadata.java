package org.iro.aiqo.submitter.apiclient.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageMetadata {

    private int size;
    private long totalElements;
    private int totalPages;
    private int number;

}
