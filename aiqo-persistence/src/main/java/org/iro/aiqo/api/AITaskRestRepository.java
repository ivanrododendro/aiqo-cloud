package org.iro.aiqo.api;

import org.iro.aiqo.model.AITask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface AITaskRestRepository extends JpaRepository<AITask, Long> {
}
