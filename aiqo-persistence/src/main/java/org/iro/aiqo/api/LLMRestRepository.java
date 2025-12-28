package org.iro.aiqo.api;

import org.iro.aiqo.model.LLM;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface LLMRestRepository extends JpaRepository<LLM, Long> {
}
