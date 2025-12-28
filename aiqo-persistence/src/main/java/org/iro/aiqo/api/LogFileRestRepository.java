package org.iro.aiqo.api;

import org.iro.aiqo.model.LogFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface LogFileRestRepository extends JpaRepository<LogFile, Long> {
}
