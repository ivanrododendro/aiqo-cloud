    package org.iro.aiqo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class Application extends MultiTenantEntity {
    @Column(nullable = false, updatable = false)
    private String name;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DBType defaultDBType;
    @Column(length = 10485760)
    private String ddlPrompt;
    @Column(length = 10485760)
    private String applicationPrompt;
    @Column(length = 10485760)
    private String configPrompt;
    @Column(length = 10485760)
    private String eventsPrompt;
    @ManyToOne
    @JoinColumn(nullable = false)
    private LLM defaultLLM;
    private Boolean scheduleForLowCost;
    private Boolean limitToFreeTier;
}
