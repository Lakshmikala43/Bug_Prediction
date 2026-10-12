package com.bugpredict.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scans")
public class Scan {
    public enum Status { PENDING, RUNNING, COMPLETED, FAILED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "total_files")
    private Integer totalFiles = 0;

    @Column(name = "scanned_files")
    private Integer scannedFiles = 0;

    @Column(name = "high_risk_count")
    private Integer highRiskCount = 0;

    @Column(name = "critical_risk_count")
    private Integer criticalRiskCount = 0;

    @Column(name = "medium_risk_count")
    private Integer mediumRiskCount = 0;

    @Column(name = "low_risk_count")
    private Integer lowRiskCount = 0;

    @Column(name = "avg_risk_score")
    private Double avgRiskScore = 0.0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Scan() {}

    public Scan(Project project) {
        this.project = project;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Integer getTotalFiles() { return totalFiles; }
    public void setTotalFiles(Integer totalFiles) { this.totalFiles = totalFiles; }

    public Integer getScannedFiles() { return scannedFiles; }
    public void setScannedFiles(Integer scannedFiles) { this.scannedFiles = scannedFiles; }

    public Integer getHighRiskCount() { return highRiskCount; }
    public void setHighRiskCount(Integer highRiskCount) { this.highRiskCount = highRiskCount; }

    public Integer getCriticalRiskCount() { return criticalRiskCount; }
    public void setCriticalRiskCount(Integer criticalRiskCount) { this.criticalRiskCount = criticalRiskCount; }

    public Integer getMediumRiskCount() { return mediumRiskCount; }
    public void setMediumRiskCount(Integer mediumRiskCount) { this.mediumRiskCount = mediumRiskCount; }

    public Integer getLowRiskCount() { return lowRiskCount; }
    public void setLowRiskCount(Integer lowRiskCount) { this.lowRiskCount = lowRiskCount; }

    public Double getAvgRiskScore() { return avgRiskScore; }
    public void setAvgRiskScore(Double avgRiskScore) { this.avgRiskScore = avgRiskScore; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
