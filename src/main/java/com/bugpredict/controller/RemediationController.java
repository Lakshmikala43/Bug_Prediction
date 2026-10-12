package com.bugpredict.controller;

import com.bugpredict.model.RemediationTask;
import com.bugpredict.repository.ProjectRepository;
import com.bugpredict.repository.RemediationTaskRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/remediation")
@CrossOrigin
public class RemediationController {

    private final RemediationTaskRepository remediationTaskRepository;
    private final ProjectRepository projectRepository;

    public RemediationController(RemediationTaskRepository remediationTaskRepository, ProjectRepository projectRepository) {
        this.remediationTaskRepository = remediationTaskRepository;
        this.projectRepository = projectRepository;
    }

    @GetMapping("/project/{projectId}")
    public List<RemediationTask> getTasks(@PathVariable Long projectId) {
        return remediationTaskRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @PostMapping("/project/{projectId}")
    public ResponseEntity<?> createTask(@PathVariable Long projectId, @RequestBody Map<String, String> payload) {
        return projectRepository.findById(projectId).map(proj -> {
            RemediationTask task = new RemediationTask();
            task.setProject(proj);
            task.setTitle(payload.getOrDefault("title", "Review High Risk Defect Code"));
            task.setDescription(payload.getOrDefault("description", ""));
            task.setRecommendationText(payload.getOrDefault("recommendation", ""));
            task.setStatus(RemediationTask.Status.OPEN);
            task.setPriority(RemediationTask.Priority.HIGH);
            task.setCreatedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(remediationTaskRepository.save(task));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam RemediationTask.Status status) {
        return remediationTaskRepository.findById(id).map(task -> {
            task.setStatus(status);
            task.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(remediationTaskRepository.save(task));
        }).orElse(ResponseEntity.notFound().build());
    }
}
