package com.bugpredict.controller;

import com.bugpredict.model.Scan;
import com.bugpredict.repository.ProjectRepository;
import com.bugpredict.repository.ScanRepository;
import com.bugpredict.service.ScanExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scans")
@CrossOrigin
public class ScanController {

    private final ScanRepository scanRepository;
    private final ProjectRepository projectRepository;
    private final ScanExecutionService scanExecutionService;

    public ScanController(ScanRepository scanRepository, ProjectRepository projectRepository, ScanExecutionService scanExecutionService) {
        this.scanRepository = scanRepository;
        this.projectRepository = projectRepository;
        this.scanExecutionService = scanExecutionService;
    }

    @GetMapping("/project/{projectId}")
    public List<Scan> getScansByProject(@PathVariable Long projectId) {
        return scanRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getScan(@PathVariable Long id) {
        return scanRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/project/{projectId}/start")
    public ResponseEntity<?> startScan(@PathVariable Long projectId) {
        return projectRepository.findById(projectId).map(proj -> {
            Scan scan = new Scan(proj);
            scan.setCreatedAt(LocalDateTime.now());
            scan = scanRepository.save(scan);
            scanExecutionService.executeScanAsync(scan.getId());
            return ResponseEntity.ok(Map.of("message", "Scan scheduled", "scanId", scan.getId()));
        }).orElse(ResponseEntity.notFound().build());
    }
}
