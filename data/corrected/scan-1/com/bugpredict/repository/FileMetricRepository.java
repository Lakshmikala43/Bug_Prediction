package com.bugpredict.repository;

import com.bugpredict.model.FileMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileMetricRepository extends JpaRepository<FileMetric, Long> {
    List<FileMetric> findByScanId(Long scanId);
    Optional<FileMetric> findByScanIdAndProjectFileId(Long scanId, Long projectFileId);
}