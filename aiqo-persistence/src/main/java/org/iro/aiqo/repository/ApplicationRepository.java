package org.iro.aiqo.repository;

import org.iro.aiqo.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findByTenantIdAndName(Integer tenantId, String name);
}
