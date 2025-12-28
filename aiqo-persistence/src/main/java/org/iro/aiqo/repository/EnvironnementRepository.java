package org.iro.aiqo.repository;

import org.iro.aiqo.model.Environment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnvironnementRepository extends JpaRepository<Environment, Long> {
    Optional<Environment> findByName(String name);
}
