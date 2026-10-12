package com.bugpredict.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_files")
public class ProjectFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "relative_path", nullable = false, length = 500)
    private String relativePath;

    @Column(name = "file_name", nullable = false, length = 150)
    private String fileName;

    @Column(name = "package_name", length = 250)
    private String packageName;

    @Column(name = "lines_of_code")
    private Integer linesOfCode = 0;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ProjectFile() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public Integer getLinesOfCode() { return linesOfCode; }
    public void setLinesOfCode(Integer linesOfCode) { this.linesOfCode = linesOfCode; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
