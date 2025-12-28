package org.iro.aiqo.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class MultiTenantEntity extends Persistable {
    @Column(nullable = false, updatable = false)
    private Integer tenantId;
}