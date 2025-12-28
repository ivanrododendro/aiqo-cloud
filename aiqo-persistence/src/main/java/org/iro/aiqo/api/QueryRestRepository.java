package org.iro.aiqo.api;

import org.iro.aiqo.model.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface QueryRestRepository extends JpaRepository<Query, Long> {
}
