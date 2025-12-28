package org.iro.aiqo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class Query extends MultiTenantEntity{
    @Column(nullable = false, updatable = false)
    private String hashcode;
    @Column(nullable = false, updatable = false, length = 10485760)
    private String sql;
    @Column(length = 255)
    private String name;
    @Column(length = 255)
    private String jobName;
    @Column(length = 10485760)
    private String comments;
    private Boolean fullyOptimized = false;
    @Column(length = 10485760)
    private String notes;
    private String logFilename;
    @ManyToOne
    @JoinColumn (nullable = false, updatable = false)
    private Application application;
    @OneToOne
    @JoinColumn (updatable = false)
    private Query originalQuery;
}
