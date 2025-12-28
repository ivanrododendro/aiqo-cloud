package org.iro.aiqo.api;

import org.iro.aiqo.model.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource
public interface EnvironnementRestRepository extends JpaRepository<Environment, Long> {
    @RestResource(path = "byName", rel = "byName")
    Environment findByName(@Param("name") String name);
}
