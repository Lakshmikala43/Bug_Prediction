package com.bugpredict.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_results")
public class PredictionResult {
    public enum RiskCategory { LOW, MEDIUM, HIGH, CRITICAL }
    public enum EngineType { RULE_BASED, MACHINE_LEARNING }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id", nullable = false)
    private Scan scan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_file_id", nullable = false)
    private ProjectFile projectFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", nullable = false, length = 20)
    private RiskCategory riskCategory = RiskCategory.LOW;

    @Column(name = "risk_score", nullable = false)
    private Double riskScore = 0.0;

    @Column(name = "confidence_score")
    private Double confidenceScore = 0.85;

    @Enumerated(EnumType.STRING)
    @Column(name = "engine_type", nullable = false, length = 30)
    private EngineType engineType = EngineType.RULE_BASED;

    @Column(name = "model_version", length = 50)
    private String modelVersion = "v1.0-explainable-rule";

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PredictionResult() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Scan getScan() { return scan; }
    public void setScan(Scan scan) { this.scan = scan; }

    public ProjectFile getProjectFile() { return projectFile; }
    public void setProjectFile(ProjectFile projectFile) { this.projectFile = projectFile; }

    public RiskCategory getRiskCategory() { return riskCategory; }
    public void setRiskCategory(RiskCategory riskCategory) { this.riskCategory = riskCategory; }

    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public EngineType getEngineType() { return engineType; }
    public void setEngineType(EngineType engineType) { this.engineType = engineType; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
