package com.bugpredict.controller;

import com.bugpredict.service.ReportExportService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin
public class ReportController {

    private final ReportExportService reportExportService;

    public ReportController(ReportExportService reportExportService) {
        this.reportExportService = reportExportService;
    }

    @GetMapping("/scan/{scanId}/csv")
    public ResponseEntity<byte[]> downloadCsv(@PathVariable Long scanId) {
        String csv = reportExportService.generateCsvReport(scanId);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"scan-" + scanId + "-risk-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping("/scan/{scanId}/download-corrected-zip")
    public ResponseEntity<Resource> downloadCorrectedZip(@PathVariable Long scanId) {
        Path zipPath = Paths.get("data", "corrected", "scan-" + scanId + "-corrected.zip");
        File zipFile = zipPath.toFile();
        if (!zipFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(zipFile);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"scan-" + scanId + "-corrected-code.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
