package com.bugpredict.repository;

import com.bugpredict.model.Scan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ScanRepository extends JpaRepository<Scan, Long> {
    List<Scan> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    Optional<Scan> findFirstByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, Scan.Status status);
}
