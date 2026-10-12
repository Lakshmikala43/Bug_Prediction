package com.bugpredict.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "defect_records")
public class DefectRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "class_name", length = 150)
    private String className;

    @Column(name = "defect_id", length = 80)
    private String defectId;

    @Column(name = "severity", length = 30)
    private String severity = "MAJOR";

    @Column(name = "is_buggy", nullable = false)
    private Boolean isBuggy = true;

    @Column(name = "commit_hash", length = 64)
    private String commitHash;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public DefectRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getDefectId() { return defectId; }
    public void setDefectId(String defectId) { this.defectId = defectId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Boolean getIsBuggy() { return isBuggy; }
    public void setIsBuggy(Boolean isBuggy) { this.isBuggy = isBuggy; }

    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}