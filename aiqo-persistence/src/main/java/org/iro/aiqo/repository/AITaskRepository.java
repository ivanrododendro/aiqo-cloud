package org.iro.aiqo.repository;

import org.iro.aiqo.model.AITask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AITaskRepository extends JpaRepository<AITask, Long> {

    @Query(value = """
                SELECT * FROM aiqo.aitask
                WHERE (processing_status = 'SCHEDULED' OR (processing_status = 'RUNNING' AND locked_at < NOW() - INTERVAL '10 minutes'))
                ORDER BY id
                LIMIT 1
            """, nativeQuery = true)
    Optional<AITask> findAndLockNextTask();
}
