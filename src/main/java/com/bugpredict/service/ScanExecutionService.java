package com.bugpredict.service;

import com.bugpredict.model.*;
import com.bugpredict.repository.*;
import com.bugpredict.service.parser.UniversalCodeScannerService;
import com.bugpredict.service.prediction.RiskPredictionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
public class ScanExecutionService {

    private final ScanRepository scanRepository;
    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final FileMetricRepository fileMetricRepository;
    private final PredictionResultRepository predictionResultRepository;
    private final UniversalCodeScannerService universalCodeScannerService;
    private final RiskPredictionService riskPredictionService;
    private final ZipArchiverService zipArchiverService;

    @Value("${bugpredict.thresholds.high-risk:65.0}")
    private double highRiskThreshold;

    @Value("${bugpredict.thresholds.critical-risk:85.0}")
    private double criticalRiskThreshold;

    private final ExecutorService scanExecutor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors())
    );

    public ScanExecutionService(ScanRepository scanRepository,
                                ProjectRepository projectRepository,
                                ProjectFileRepository projectFileRepository,
                                FileMetricRepository fileMetricRepository,
                                PredictionResultRepository predictionResultRepository,
                                UniversalCodeScannerService universalCodeScannerService,
                                RiskPredictionService riskPredictionService,
                                ZipArchiverService zipArchiverService) {
        this.scanRepository = scanRepository;
        this.projectRepository = projectRepository;
        this.projectFileRepository = projectFileRepository;
        this.fileMetricRepository = fileMetricRepository;
        this.predictionResultRepository = predictionResultRepository;
        this.universalCodeScannerService = universalCodeScannerService;
        this.riskPredictionService = riskPredictionService;
        this.zipArchiverService = zipArchiverService;
    }

    private static final Set<String> IGNORED_DIRS = Set.of(
            "node_modules", ".git", ".github", "target", "build", "dist", ".next", ".cache", "vendor",
            ".idea", ".vscode", "obj", "bin", "__pycache__", ".venv", "venv", "env", "coverage", ".turbo", ".nuxt"
    );

    private static class ProcessedFileResult {
        ProjectFile projectFile;
        FileMetric metric;
        PredictionResult predictionResult;
        Path targetCorrectedFile;
        String correctedCode;
    }

    @Async
    public void executeScanAsync(Long scanId) {
        executeScanSync(scanId);
    }

    public void executeScanSync(Long scanId) {
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

            // Rapid Directory Traversal
            List<Path> codePaths = new ArrayList<>();
            Files.walkFileTree(sourceDir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    if (IGNORED_DIRS.contains(name.toLowerCase())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (universalCodeScannerService.isSupportedSourceFile(file.getFileName().toString())) {
                        codePaths.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            scan.setTotalFiles(codePaths.size());
            scanRepository.save(scan);

            Path correctedDir = Paths.get("data", "corrected", "scan-" + scanId);
            Files.createDirectories(correctedDir);

            // Parallel file analysis
            List<CompletableFuture<ProcessedFileResult>> futures = new ArrayList<>(codePaths.size());

            for (Path p : codePaths) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    String relativePath = sourceDir.toPath().relativize(p).toString().replace('\\', '/');
                    File file = p.toFile();

                    ProjectFile pf = new ProjectFile();
                    pf.setProject(project);
                    pf.setRelativePath(relativePath);
                    pf.setFileName(file.getName());

                    UniversalCodeScannerService.CodeAnalysisResult analysis = universalCodeScannerService.analyzeAndAutoCorrect(file);

                    pf.setPackageName(analysis.language);
                    pf.setLinesOfCode(analysis.loc);
                    pf.setFileSizeBytes(file.length());

                    FileMetric metric = new FileMetric();
                    metric.setScan(scan);
                    metric.setProjectFile(pf);
                    metric.setLoc(analysis.loc);
                    metric.setSloc(analysis.sloc);
                    metric.setClassCount(1);
                    metric.setMethodCount(Math.max(1, analysis.loc / 25));
                    metric.setCyclomaticComplexity(analysis.cyclomaticComplexity);
                    metric.setMaxNestingDepth(analysis.maxNestingDepth);
                    metric.setDefectHistoryCount(analysis.bugCount);
                    metric.setCommitCount(Math.max(1, analysis.bugCount * 2));
                    metric.setChurnLines(analysis.bugCount * 30);

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

                    PredictionResult pr = new PredictionResult();
                    pr.setScan(scan);
                    pr.setProjectFile(pf);
                    pr.setRiskScore(eval.getRiskScore());
                    pr.setRiskCategory(eval.getCategory());
                    pr.setConfidenceScore(eval.getConfidence());
                    pr.setExplanation(bugExplanation);
                    pr.setEngineType(PredictionResult.EngineType.RULE_BASED);

                    ProcessedFileResult res = new ProcessedFileResult();
                    res.projectFile = pf;
                    res.metric = metric;
                    res.predictionResult = pr;
                    res.targetCorrectedFile = correctedDir.resolve(relativePath);
                    res.correctedCode = analysis.correctedCode;
                    return res;
                }, scanExecutor));
            }

            // Wait for all parallel file analyses
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            List<ProjectFile> projectFilesToSave = new ArrayList<>(futures.size());
            List<FileMetric> metricsToSave = new ArrayList<>(futures.size());
            List<PredictionResult> predictionsToSave = new ArrayList<>(futures.size());

            int lowCount = 0;
            int medCount = 0;
            int highCount = 0;
            int critCount = 0;
            double totalScore = 0.0;

            for (CompletableFuture<ProcessedFileResult> fut : futures) {
                ProcessedFileResult res = fut.get();
                projectFilesToSave.add(res.projectFile);
                metricsToSave.add(res.metric);
                predictionsToSave.add(res.predictionResult);

                // Write file to disk
                if (res.targetCorrectedFile.getParent() != null) {
                    Files.createDirectories(res.targetCorrectedFile.getParent());
                }
                Files.writeString(res.targetCorrectedFile, res.correctedCode, StandardCharsets.UTF_8);

                totalScore += res.predictionResult.getRiskScore();
                switch (res.predictionResult.getRiskCategory()) {
                    case CRITICAL -> critCount++;
                    case HIGH -> highCount++;
                    case MEDIUM -> medCount++;
                    case LOW -> lowCount++;
                }
            }

            // Bulk Save to Database in 3 fast transactions
            projectFileRepository.saveAll(projectFilesToSave);
            fileMetricRepository.saveAll(metricsToSave);
            predictionResultRepository.saveAll(predictionsToSave);

            // Zip the clean corrected archive
            Path correctedZip = Paths.get("data", "corrected", "scan-" + scanId + "-corrected.zip");
            zipArchiverService.zipDirectory(correctedDir, correctedZip);

            scan.setScannedFiles(codePaths.size());
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
