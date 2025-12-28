package org.iro.aiqo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetTime;
import java.util.Currency;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class LLM extends MultiTenantEntity {
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LLMFamily family;
    private String provider;
    @Column(nullable = false)
    private String modelName;
    private Long inputTokenCost;
    private Long cachedInputTokenCost;
    private Long outputTokenCost;
    private Currency currency;
    private Integer maxInputTokens;
    private Integer maxOutputTokens;
    @Column(nullable = false, length = 10485760)
    private String mainPrompt;
    @Column(nullable = false, length = 10485760)
    private String structurePrompt;
    private Integer freeTierTPM;
    private String apiUrl;
    private String apiKey;
    private OffsetTime lowCostStart;
    private OffsetTime lowCostStop;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LLMVendor llmVendor;


    public boolean hasLowcostFare() {
        return lowCostStart != null && lowCostStop != null;
    }

    public boolean hasFreeTier() {
        return freeTierTPM != null;
    }
}
