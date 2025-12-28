package org.iro.aiqo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.ZonedDateTime;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class Run extends MultiTenantEntity {
    @Column(nullable = false, updatable = false)
    private Long duration;
    @Column(nullable = false, updatable = false)
    private Double cost;
    @Column(updatable = false)
    private Double startupCost;
    @Column(updatable = false)
    private Long rows;
    // Buffers metrics
    @Column(updatable = false)
    private Long buffersSharedHit;
    @Column(updatable = false)
    private Long buffersSharedRead;
    @Column(updatable = false)
    private Long buffersSharedDirtied;
    @Column(updatable = false)
    private Long buffersSharedWritten;
    @Column(updatable = false)
    private Long buffersTempRead;
    @Column(updatable = false)
    private Long buffersTempWritten;
    // WAL metrics
    @Column(updatable = false)
    private Long walRecords;
    @Column(updatable = false)
    private Long walFpi;
    @Column(updatable = false)
    private Long walBytes;
    @Column(nullable = false, updatable = false, length = 10485760)
    private String plan;
    @Column(nullable = false, updatable = false)
    private ZonedDateTime startTime;
    @Column(nullable = false, updatable = false)
    private ZonedDateTime endTime;
    @ManyToOne
    @JoinColumn(nullable = false)
    private Environment environnement;
    @OneToOne
    @JoinColumn(nullable = false)
    private Query query;
    @OneToMany(mappedBy = "run")
    private List<AIHint> aiHints;
}
