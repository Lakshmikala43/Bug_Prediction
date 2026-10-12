package com.bugpredict.controller;

import com.bugpredict.model.PredictionResult;
import com.bugpredict.model.Project;
import com.bugpredict.model.Scan;
import com.bugpredict.repository.PredictionResultRepository;
import com.bugpredict.repository.ProjectRepository;
import com.bugpredict.repository.ScanRepository;
import com.bugpredict.service.parser.UniversalCodeScannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/api/workflow")
@CrossOrigin
public class WorkflowController {

    private final ProjectRepository projectRepository;
    private final ScanRepository scanRepository;
    private final PredictionResultRepository predictionResultRepository;
    private final UniversalCodeScannerService scannerService;

    public WorkflowController(ProjectRepository projectRepository,
                              ScanRepository scanRepository,
                              PredictionResultRepository predictionResultRepository,
                              UniversalCodeScannerService scannerService) {
        this.projectRepository = projectRepository;
        this.scanRepository = scanRepository;
        this.predictionResultRepository = predictionResultRepository;
        this.scannerService = scannerService;
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
                    detail.originalCode = java.nio.file.Files.readString(file.toPath());
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
        resp.put("downloadZipUrl", "/api/reports/scan/" + scan.getId() + "/download-corrected-zip");

        return ResponseEntity.ok(resp);
    }
}
