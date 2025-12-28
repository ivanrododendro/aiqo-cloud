package org.iro.aiqo.repository;

import org.iro.aiqo.model.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface QueryRepository extends JpaRepository<Query, Long> {
    Optional<Query> findByTenantIdAndHashcode(Integer tenantId, String hashcode);
}
