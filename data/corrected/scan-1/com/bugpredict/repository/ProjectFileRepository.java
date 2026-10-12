package com.bugpredict.repository;

import com.bugpredict.model.ProjectFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {
    List<ProjectFile> findByProjectId(Long projectId);
    Optional<ProjectFile> findByProjectIdAndRelativePath(Long projectId, String relativePath);
}