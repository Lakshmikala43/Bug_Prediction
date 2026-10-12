package com.bugpredict.repository;

import com.bugpredict.model.PredictionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PredictionResultRepository extends JpaRepository<PredictionResult, Long> {
    List<PredictionResult> findByScanIdOrderByRiskScoreDesc(Long scanId);
    Optional<PredictionResult> findByScanIdAndProjectFileId(Long scanId, Long projectFileId);
    long countByScanIdAndRiskCategory(Long scanId, PredictionResult.RiskCategory riskCategory);
}
