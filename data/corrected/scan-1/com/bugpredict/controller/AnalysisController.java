package com.bugpredict.controller;

import com.bugpredict.model.FileMetric;
import com.bugpredict.model.PredictionResult;
import com.bugpredict.repository.FileMetricRepository;
import com.bugpredict.repository.PredictionResultRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin
public class AnalysisController {

    private final PredictionResultRepository predictionResultRepository;
    private final FileMetricRepository fileMetricRepository;

    public AnalysisController(PredictionResultRepository predictionResultRepository, FileMetricRepository fileMetricRepository) {
        this.predictionResultRepository = predictionResultRepository;
        this.fileMetricRepository = fileMetricRepository;
    }

    @GetMapping("/scan/{scanId}/predictions")
    public List<PredictionResult> getPredictions(@PathVariable Long scanId) {
        return predictionResultRepository.findByScanIdOrderByRiskScoreDesc(scanId);
    }

    @GetMapping("/scan/{scanId}/metrics")
    public List<FileMetric> getMetrics(@PathVariable Long scanId) {
        return fileMetricRepository.findByScanId(scanId);
    }
}