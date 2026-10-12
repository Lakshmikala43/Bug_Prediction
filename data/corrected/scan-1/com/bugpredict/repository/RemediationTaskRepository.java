package com.bugpredict.repository;

import com.bugpredict.model.RemediationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RemediationTaskRepository extends JpaRepository<RemediationTask, Long> {
    List<RemediationTask> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<RemediationTask> findByAssignedToId(Long assignedToId);
}