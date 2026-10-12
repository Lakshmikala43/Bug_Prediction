package com.bugpredict.controller;

import com.bugpredict.model.PredictionResult;
import com.bugpredict.model.Project;
import com.bugpredict.model.Scan;
import com.bugpredict.repository.PredictionResultRepository;
import com.bugpredict.repository.ProjectRepository;
import com.bugpredict.repository.ScanRepository;
import com.bugpredict.service.ZipArchiverService;
import com.bugpredict.service.parser.UniversalCodeScannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/workflow")
@CrossOrigin
public class WorkflowController {

    private final ProjectRepository projectRepository;
    private final ScanRepository scanRepository;
    private final PredictionResultRepository predictionResultRepository;
    private final UniversalCodeScannerService scannerService;
    private final ZipArchiverService zipArchiverService;

    public WorkflowController(ProjectRepository projectRepository,
                              ScanRepository scanRepository,
                              PredictionResultRepository predictionResultRepository,
                              UniversalCodeScannerService scannerService,
                              ZipArchiverService zipArchiverService) {
        this.projectRepository = projectRepository;
        this.scanRepository = scanRepository;
        this.predictionResultRepository = predictionResultRepository;
        this.scannerService = scannerService;
        this.zipArchiverService = zipArchiverService;
    }

    public static class FileBugDetail {
        public String fileName;
        public String relativePath;
        public String language;
        public double riskScore;
        public String riskCategory;
        public List<UniversalCodeScannerService.DetectedBug> bugs = new ArrayList<>();
        public String originalCode = "";
        public String correctedCode = "";
    }

    @GetMapping("/scan/{scanId}/details")
    public ResponseEntity<?> getScanDetails(@PathVariable Long scanId) {
        Scan scan = scanRepository.findById(scanId).orElse(null);
        if (scan == null) return ResponseEntity.notFound().build();

        Project project = scan.getProject();
        File sourceDir = new File(project.getSourcePath() != null ? project.getSourcePath() : "");

        List<PredictionResult> predictions = predictionResultRepository.findByScanIdOrderByRiskScoreDesc(scanId);
        List<FileBugDetail> fileDetails = new ArrayList<>();

        int totalBugs = 0;

        for (PredictionResult pr : predictions) {
            FileBugDetail detail = new FileBugDetail();
            detail.fileName = pr.getProjectFile().getFileName();
            detail.relativePath = pr.getProjectFile().getRelativePath();
            detail.riskScore = pr.getRiskScore();
            detail.riskCategory = pr.getRiskCategory().name();

            File file = new File(sourceDir, detail.relativePath);
            if (file.exists()) {
                UniversalCodeScannerService.CodeAnalysisResult analysis = scannerService.analyzeAndAutoCorrect(file);
                detail.language = analysis.language;
                detail.bugs = analysis.detectedBugs;
                detail.correctedCode = analysis.correctedCode;
                totalBugs += analysis.detectedBugs.size();
                try {
                    detail.originalCode = Files.readString(file.toPath());
                } catch (Exception ignored) {}
            }
            fileDetails.add(detail);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("scanId", scan.getId());
        resp.put("projectName", project.getName());
        resp.put("status", scan.getStatus().name());
        resp.put("scannedFiles", scan.getScannedFiles());
        resp.put("totalBugs", totalBugs);
        resp.put("avgRiskScore", scan.getAvgRiskScore());
        resp.put("files", fileDetails);

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/scan/{scanId}/apply-corrections")
    public ResponseEntity<?> applyCorrectionsAndGenerateZip(@PathVariable Long scanId) {
        Scan scan = scanRepository.findById(scanId).orElse(null);
        if (scan == null) return ResponseEntity.notFound().build();

        Project project = scan.getProject();
        File sourceDir = new File(project.getSourcePath() != null ? project.getSourcePath() : "");

        try {
            Path correctedDir = Paths.get("data", "corrected", "scan-" + scanId);
            Files.createDirectories(correctedDir);

            List<PredictionResult> predictions = predictionResultRepository.findByScanIdOrderByRiskScoreDesc(scanId);
            int correctedCount = 0;

            for (PredictionResult pr : predictions) {
                String relPath = pr.getProjectFile().getRelativePath();
                File srcFile = new File(sourceDir, relPath);
                if (srcFile.exists()) {
                    UniversalCodeScannerService.CodeAnalysisResult analysis = scannerService.analyzeAndAutoCorrect(srcFile);
                    Path targetFile = correctedDir.resolve(relPath);
                    if (targetFile.getParent() != null) {
                        Files.createDirectories(targetFile.getParent());
                    }
                    Files.writeString(targetFile, analysis.correctedCode, StandardCharsets.UTF_8);
                    correctedCount += analysis.detectedBugs.size();
                }
            }

            Path correctedZip = Paths.get("data", "corrected", "scan-" + scanId + "-corrected.zip");
            zipArchiverService.zipDirectory(correctedDir, correctedZip);

            return ResponseEntity.ok(Map.of(
                    "message", "All detected bugs successfully corrected!",
                    "correctedBugsCount", correctedCount,
                    "downloadUrl", "/api/reports/scan/" + scanId + "/download-corrected-zip"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to generate corrected ZIP: " + e.getMessage()));
        }
    }
}
