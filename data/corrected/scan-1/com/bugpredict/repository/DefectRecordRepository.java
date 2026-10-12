package com.bugpredict.repository;

import com.bugpredict.model.DefectRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DefectRecordRepository extends JpaRepository<DefectRecord, Long> {
    List<DefectRecord> findByProjectId(Long projectId);
    List<DefectRecord> findByProjectIdAndFilePath(Long projectId, String filePath);
    long countByProjectId(Long projectId);
}