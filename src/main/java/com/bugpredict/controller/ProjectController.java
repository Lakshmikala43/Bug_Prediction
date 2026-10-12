package com.bugpredict.controller;

import com.bugpredict.model.Project;
import com.bugpredict.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin
public class ProjectController {

    private final ProjectRepository projectRepository;

    public ProjectController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
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
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return projectRepository.save(p);
    }
}
