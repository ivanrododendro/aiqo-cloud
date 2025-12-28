package org.iro.aiqo.api;

import org.iro.aiqo.model.AIHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface AiHintRestRepository extends JpaRepository<AIHint, Long> {
}
