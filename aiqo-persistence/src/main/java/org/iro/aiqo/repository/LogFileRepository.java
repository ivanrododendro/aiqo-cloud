package org.iro.aiqo.repository;

import org.iro.aiqo.model.LogFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogFileRepository extends JpaRepository<LogFile, Long> {
}
