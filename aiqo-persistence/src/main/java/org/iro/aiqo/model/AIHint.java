package org.iro.aiqo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class AIHint extends MultiTenantEntity {
    @Column(name = "title", nullable = false, updatable = false, length = 10485760)
    private String title;
    @Column(name = "detail", nullable = false, updatable = false, length = 10485760)
    private String detail;
    @Column(name = "severity", nullable = false, updatable = true, length = 15)
    private String severity;
    @Column(name = "estimatedImprovement", nullable = false, updatable = false, length = 10485760)
    private String estimatedImprovement;
    private Boolean tested = false;
    private Boolean provenUseful = false;
    @Column(length = 10485760)
    private String notes;
    @ManyToOne
    @JoinColumn(name = "run_id")
    private Run run;
    @ManyToOne
    @JoinColumn(name = "aitask_id")
    private AITask aiTask;
}
