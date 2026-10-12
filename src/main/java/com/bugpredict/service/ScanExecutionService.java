package com.bugpredict.service;

import com.bugpredict.model.*;
import com.bugpredict.repository.*;
import com.bugpredict.service.parser.JavaAstParserService;
import com.bugpredict.service.prediction.RiskPredictionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ScanExecutionService {

    private final ScanRepository scanRepository;
    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final FileMetricRepository fileMetricRepository;
    private final PredictionResultRepository predictionResultRepository;
    private final DefectRecordRepository defectRecordRepository;
    private final JavaAstParserService astParserService;
    private final RiskPredictionService riskPredictionService;

    @Value("${bugpredict.thresholds.high-risk:65.0}")
    private double highRiskThreshold;

    @Value("${bugpredict.thresholds.critical-risk:85.0}")
    private double criticalRiskThreshold;

    public ScanExecutionService(ScanRepository scanRepository,
                                ProjectRepository projectRepository,
                                ProjectFileRepository projectFileRepository,
                                FileMetricRepository fileMetricRepository,
                                PredictionResultRepository predictionResultRepository,
                                DefectRecordRepository defectRecordRepository,
                                JavaAstParserService astParserService,
                                RiskPredictionService riskPredictionService) {
        this.scanRepository = scanRepository;
        this.projectRepository = projectRepository;
        this.projectFileRepository = projectFileRepository;
        this.fileMetricRepository = fileMetricRepository;
        this.predictionResultRepository = predictionResultRepository;
        this.defectRecordRepository = defectRecordRepository;
        this.astParserService = astParserService;
        this.riskPredictionService = riskPredictionService;
    }

    @Async
    @Transactional
    public void executeScanAsync(Long scanId) {
        Scan scan = scanRepository.findById(scanId).orElse(null);
        if (scan == null) return;

        scan.setStatus(Scan.Status.RUNNING);
        scan.setStartedAt(LocalDateTime.now());
        scanRepository.save(scan);

        Project project = scan.getProject();
        File sourceDir = new File(project.getSourcePath() != null ? project.getSourcePath() : "");

        try {
            if (!sourceDir.exists() || !sourceDir.isDirectory()) {
                scan.setStatus(Scan.Status.FAILED);
                scan.setErrorMessage("Project source directory does not exist: " + project.getSourcePath());
                scanRepository.save(scan);
                return;
            }

            // Discover all .java files safely excluding target, build, .git, etc.
            List<Path> javaPaths;
            try (Stream<Path> stream = Files.walk(sourceDir.toPath())) {
                javaPaths = stream
                        .filter(p -> p.toString().endsWith(".java"))
                        .filter(p -> !p.toString().contains("target") && !p.toString().contains(".git") && !p.toString().contains("build"))
                        .toList();
            }

            scan.setTotalFiles(javaPaths.size());
            scanRepository.save(scan);

            int lowCount = 0;
            int medCount = 0;
            int highCount = 0;
            int critCount = 0;
            double totalScore = 0.0;

            for (Path p : javaPaths) {
                String relativePath = sourceDir.toPath().relativize(p).toString().replace('\\', '/');
                File file = p.toFile();

                // Find or create ProjectFile
                ProjectFile projectFile = projectFileRepository.findByProjectIdAndRelativePath(project.getId(), relativePath)
                        .orElseGet(() -> {
                            ProjectFile pf = new ProjectFile();
                            pf.setProject(project);
                            pf.setRelativePath(relativePath);
                            pf.setFileName(file.getName());
                            return pf;
                        });

                JavaAstParserService.FileAstMetrics ast = astParserService.parseJavaFile(file);
                projectFile.setPackageName(ast.packageName);
                projectFile.setLinesOfCode(ast.loc);
                projectFile.setFileSizeBytes(file.length());
                projectFile = projectFileRepository.save(projectFile);

                // Check defect history for this file
                List<DefectRecord> defects = defectRecordRepository.findByProjectIdAndFilePath(project.getId(), relativePath);

                FileMetric metric = new FileMetric();
                metric.setScan(scan);
                metric.setProjectFile(projectFile);
                metric.setLoc(ast.loc);
                metric.setSloc(ast.sloc);
                metric.setClassCount(ast.classCount);
                metric.setMethodCount(ast.methodCount);
                metric.setCyclomaticComplexity(ast.cyclomaticComplexity);
                metric.setMaxMethodLength(ast.maxMethodLength);
                metric.setMaxNestingDepth(ast.maxNestingDepth);
                metric.setImportCount(ast.importCount);
                metric.setDefectHistoryCount(defects.size());
                metric.setCommitCount(Math.max(1, defects.size() * 2));
                metric.setChurnLines(defects.size() * 45);
                metric = fileMetricRepository.save(metric);

                // Run Risk Prediction
                RiskPredictionService.PredictionEvaluation eval = riskPredictionService.predictRisk(metric, highRiskThreshold, criticalRiskThreshold);

                PredictionResult result = new PredictionResult();
                result.setScan(scan);
                result.setProjectFile(projectFile);
                result.setRiskScore(eval.getRiskScore());
                result.setRiskCategory(eval.getCategory());
                result.setConfidenceScore(eval.getConfidence());
                result.setExplanation(eval.getExplanation());
                result.setEngineType(PredictionResult.EngineType.RULE_BASED);
                predictionResultRepository.save(result);

                totalScore += eval.getRiskScore();
                switch (eval.getCategory()) {
                    case CRITICAL -> critCount++;
                    case HIGH -> highCount++;
                    case MEDIUM -> medCount++;
                    case LOW -> lowCount++;
                }

                scan.setScannedFiles(scan.getScannedFiles() + 1);
            }

            scan.setLowRiskCount(lowCount);
            scan.setMediumRiskCount(medCount);
            scan.setHighRiskCount(highCount);
            scan.setCriticalRiskCount(critCount);
            scan.setAvgRiskScore(javaPaths.isEmpty() ? 0.0 : Math.round((totalScore / javaPaths.size()) * 10.0) / 10.0);
            scan.setStatus(Scan.Status.COMPLETED);
            scan.setCompletedAt(LocalDateTime.now());

            project.setFileCount(javaPaths.size());
            projectRepository.save(project);
            scanRepository.save(scan);

        } catch (Exception ex) {
            scan.setStatus(Scan.Status.FAILED);
            scan.setErrorMessage("Scan failed: " + ex.getMessage());
            scanRepository.save(scan);
        }
    }
}
