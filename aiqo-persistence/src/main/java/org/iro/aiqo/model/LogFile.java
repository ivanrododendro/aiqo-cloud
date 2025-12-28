package org.iro.aiqo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class LogFile extends MultiTenantEntity {
    private String fullFilename;
    private String dbType;
    private int dbMajorVersion;
    private int dbMinorVersion;
}
