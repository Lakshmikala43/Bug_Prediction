package com.bugpredict.service;

import com.bugpredict.model.*;
import com.bugpredict.repository.*;
import com.bugpredict.service.parser.UniversalCodeScannerService;
import com.bugpredict.service.prediction.RiskPredictionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final UniversalCodeScannerService universalCodeScannerService;
    private final RiskPredictionService riskPredictionService;
    private final ZipArchiverService zipArchiverService;

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
                                UniversalCodeScannerService universalCodeScannerService,
                                RiskPredictionService riskPredictionService,
                                ZipArchiverService zipArchiverService) {
        this.scanRepository = scanRepository;
        this.projectRepository = projectRepository;
        this.projectFileRepository = projectFileRepository;
        this.fileMetricRepository = fileMetricRepository;
        this.predictionResultRepository = predictionResultRepository;
        this.defectRecordRepository = defectRecordRepository;
        this.universalCodeScannerService = universalCodeScannerService;
        this.riskPredictionService = riskPredictionService;
        this.zipArchiverService = zipArchiverService;
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

            // Discover all supported code files across any language (Java, Python, JS, C++, Go, etc.)
            List<Path> codePaths;
            try (Stream<Path> stream = Files.walk(sourceDir.toPath())) {
                codePaths = stream
                        .filter(p -> !Files.isDirectory(p))
                        .filter(p -> universalCodeScannerService.isSupportedSourceFile(p.getFileName().toString()))
                        .filter(p -> !p.toString().contains("target") && !p.toString().contains(".git") && !p.toString().contains("build") && !p.toString().contains("node_modules"))
                        .toList();
            }

            scan.setTotalFiles(codePaths.size());
            scanRepository.save(scan);

            int lowCount = 0;
            int medCount = 0;
            int highCount = 0;
            int critCount = 0;
            double totalScore = 0.0;

            Path correctedDir = Paths.get("data", "corrected", "scan-" + scanId);
            Files.createDirectories(correctedDir);

            for (Path p : codePaths) {
                String relativePath = sourceDir.toPath().relativize(p).toString().replace('\\', '/');
                File file = p.toFile();

                ProjectFile projectFile = projectFileRepository.findByProjectIdAndRelativePath(project.getId(), relativePath)
                        .orElseGet(() -> {
                            ProjectFile pf = new ProjectFile();
                            pf.setProject(project);
                            pf.setRelativePath(relativePath);
                            pf.setFileName(file.getName());
                            return pf;
                        });

                UniversalCodeScannerService.CodeAnalysisResult analysis = universalCodeScannerService.analyzeAndAutoCorrect(file);

                projectFile.setPackageName(analysis.language);
                projectFile.setLinesOfCode(analysis.loc);
                projectFile.setFileSizeBytes(file.length());
                projectFile = projectFileRepository.save(projectFile);

                // Save auto-corrected code to corrected directory tree
                Path targetCorrectedFile = correctedDir.resolve(relativePath);
                if (targetCorrectedFile.getParent() != null) {
                    Files.createDirectories(targetCorrectedFile.getParent());
                }
                Files.writeString(targetCorrectedFile, analysis.correctedCode, StandardCharsets.UTF_8);

                List<DefectRecord> defects = defectRecordRepository.findByProjectIdAndFilePath(project.getId(), relativePath);

                FileMetric metric = new FileMetric();
                metric.setScan(scan);
                metric.setProjectFile(projectFile);
                metric.setLoc(analysis.loc);
                metric.setSloc(analysis.sloc);
                metric.setClassCount(1);
                metric.setMethodCount(Math.max(1, analysis.loc / 25));
                metric.setCyclomaticComplexity(analysis.cyclomaticComplexity);
                metric.setMaxNestingDepth(analysis.maxNestingDepth);
                metric.setDefectHistoryCount(defects.size() + analysis.bugCount);
                metric.setCommitCount(Math.max(1, analysis.bugCount * 2));
                metric.setChurnLines(analysis.bugCount * 30);
                metric = fileMetricRepository.save(metric);

                // Predict risk
                RiskPredictionService.PredictionEvaluation eval = riskPredictionService.predictRisk(metric, highRiskThreshold, criticalRiskThreshold);

                String bugExplanation = eval.getExplanation();
                if (!analysis.detectedBugs.isEmpty()) {
                    StringBuilder sb = new StringBuilder(bugExplanation);
                    sb.append(" | [Auto-Detected Bugs]: ");
                    for (UniversalCodeScannerService.DetectedBug b : analysis.detectedBugs) {
                        sb.append(String.format("Line %d: %s (%s, %s); ", b.lineNumber, b.ruleName, b.severity, b.message));
                    }
                    bugExplanation = sb.toString();
                }

                PredictionResult result = new PredictionResult();
                result.setScan(scan);
                result.setProjectFile(projectFile);
                result.setRiskScore(eval.getRiskScore());
                result.setRiskCategory(eval.getCategory());
                result.setConfidenceScore(eval.getConfidence());
                result.setExplanation(bugExplanation);
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

            // Create Corrected ZIP Archive
            Path correctedZip = Paths.get("data", "corrected", "scan-" + scanId + "-corrected.zip");
            zipArchiverService.zipDirectory(correctedDir, correctedZip);

            scan.setLowRiskCount(lowCount);
            scan.setMediumRiskCount(medCount);
            scan.setHighRiskCount(highCount);
            scan.setCriticalRiskCount(critCount);
            scan.setAvgRiskScore(codePaths.isEmpty() ? 0.0 : Math.round((totalScore / codePaths.size()) * 10.0) / 10.0);
            scan.setStatus(Scan.Status.COMPLETED);
            scan.setCompletedAt(LocalDateTime.now());

            project.setFileCount(codePaths.size());
            projectRepository.save(project);
            scanRepository.save(scan);

        } catch (Exception ex) {
            scan.setStatus(Scan.Status.FAILED);
            scan.setErrorMessage("Scan failed: " + ex.getMessage());
            scanRepository.save(scan);
        }
    }
}