package org.iro.aiqo.api;

import org.iro.aiqo.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.Optional;

@RepositoryRestResource
public interface ApplicationRestRepository extends JpaRepository<Application, Long> {
    @RestResource(path = "byNameAndTenant", rel = "byNameAndTenant")
    Optional<Application> findByNameAndTenantId(@Param("name") String name, @Param("tenantId") Integer tenantId);
}
