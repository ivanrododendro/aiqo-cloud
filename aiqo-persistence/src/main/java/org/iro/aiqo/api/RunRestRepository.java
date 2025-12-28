package org.iro.aiqo.api;

import org.iro.aiqo.model.Run;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface RunRestRepository extends JpaRepository<Run, Long> {
}
