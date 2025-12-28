package org.iro.aiqo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class AITask extends  MultiTenantEntity{
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private Long inputTokens;
    private Long outputTokens;
    private Long estimatedCost;
    private OffsetDateTime lockedAt;
    private Currency currency;
    @Column(name = "summary", length = 10485760)
    private String summary;
    @Column(name = "diagnosis", length = 10485760)
    private String diagnosis;
    @OneToOne
    @JoinColumn(name = "run_id", nullable = false, updatable = false)
    private Run run;
    @OneToOne
    private LLM LLMServed;
    @OneToMany(mappedBy = "aiTask")
    private List<AIHint> aiHints;
}
