package org.iro.aiqo.repository;

import org.iro.aiqo.model.LLM;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LLMRepository extends JpaRepository<LLM, Long> {
}
