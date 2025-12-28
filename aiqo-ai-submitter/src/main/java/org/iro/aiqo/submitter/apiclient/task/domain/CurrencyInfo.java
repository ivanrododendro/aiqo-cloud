package org.iro.aiqo.submitter.apiclient.task.domain;

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
public class CurrencyInfo {

    private String currencyCode;
    private Integer numericCode;
    private String numericCodeAsString;
    private String displayName;
    private String symbol;
    private Integer defaultFractionDigits;

}
