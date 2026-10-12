package com.bugpredict.repository;

import com.bugpredict.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByIsArchivedFalseOrderByCreatedAtDesc();
    List<Project> findByOwnerIdAndIsArchivedFalse(Long ownerId);
}