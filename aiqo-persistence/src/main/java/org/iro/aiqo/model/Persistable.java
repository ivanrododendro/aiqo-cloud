package org.iro.aiqo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@MappedSuperclass
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class Persistable {
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @Column(nullable = false, updatable = false)
    private String userCreated;

    @JsonIgnore
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @JsonIgnore
    @Column(nullable = false)
    private String userUpdated;

    @JsonIgnore
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        updatedAt = createdAt;
        userCreated = "SYSTEM";
        userUpdated = userCreated;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        userUpdated = "SYSTEM";
    }
}
