package com.bugpredict.controller;

import com.bugpredict.model.Project;
import com.bugpredict.repository.ProjectRepository;
import com.bugpredict.service.FileUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final FileUploadService fileUploadService;

    public ProjectController(ProjectRepository projectRepository, FileUploadService fileUploadService) {
        this.projectRepository = projectRepository;
        this.fileUploadService = fileUploadService;
    }

    @GetMapping
    public List<Project> getAllProjects() {
        return projectRepository.findByIsArchivedFalseOrderByCreatedAtDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(@PathVariable Long id) {
        return projectRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Project createProject(@RequestBody Map<String, String> payload) {
        Project p = new Project();
        p.setName(payload.getOrDefault("name", "Untitled Project"));
        p.setDescription(payload.getOrDefault("description", ""));
        p.setSourcePath(payload.getOrDefault("sourcePath", ""));
        p.setLanguage(payload.getOrDefault("language", "Multi-Language"));
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return projectRepository.save(p);
    }

    @PostMapping("/upload-zip")
    public ResponseEntity<?> uploadZipProject(@RequestParam("name") String name,
                                              @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty"));
        }
        try {
            String uploadId = UUID.randomUUID().toString().substring(0, 8);
            Path targetDir = Paths.get("data", "uploads", uploadId);
            fileUploadService.extractZipArchive(file, targetDir);

            Project p = new Project();
            p.setName(name);
            p.setDescription("Uploaded ZIP archive (" + file.getOriginalFilename() + ")");
            p.setSourcePath(targetDir.toAbsolutePath().toString());
            p.setLanguage("Multi-Language");
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            Project saved = projectRepository.save(p);

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to extract ZIP: " + e.getMessage()));
        }
    }
}
