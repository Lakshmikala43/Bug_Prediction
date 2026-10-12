package com.bugpredict.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_metrics")
public class FileMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "scan_id", nullable = false)
    private Scan scan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_file_id", nullable = false)
    private ProjectFile projectFile;

    @Column(name = "loc")
    private Integer loc = 0;

    @Column(name = "sloc")
    private Integer sloc = 0;

    @Column(name = "class_count")
    private Integer classCount = 0;

    @Column(name = "method_count")
    private Integer methodCount = 0;

    @Column(name = "cyclomatic_complexity")
    private Integer cyclomaticComplexity = 0;

    @Column(name = "max_method_length")
    private Integer maxMethodLength = 0;

    @Column(name = "max_nesting_depth")
    private Integer maxNestingDepth = 0;

    @Column(name = "import_count")
    private Integer importCount = 0;

    @Column(name = "commit_count")
    private Integer commitCount = 0;

    @Column(name = "churn_lines")
    private Integer churnLines = 0;

    @Column(name = "defect_history_count")
    private Integer defectHistoryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public FileMetric() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Scan getScan() { return scan; }
    public void setScan(Scan scan) { this.scan = scan; }

    public ProjectFile getProjectFile() { return projectFile; }
    public void setProjectFile(ProjectFile projectFile) { this.projectFile = projectFile; }

    public Integer getLoc() { return loc; }
    public void setLoc(Integer loc) { this.loc = loc; }

    public Integer getSloc() { return sloc; }
    public void setSloc(Integer sloc) { this.sloc = sloc; }

    public Integer getClassCount() { return classCount; }
    public void setClassCount(Integer classCount) { this.classCount = classCount; }

    public Integer getMethodCount() { return methodCount; }
    public void setMethodCount(Integer methodCount) { this.methodCount = methodCount; }

    public Integer getCyclomaticComplexity() { return cyclomaticComplexity; }
    public void setCyclomaticComplexity(Integer cyclomaticComplexity) { this.cyclomaticComplexity = cyclomaticComplexity; }

    public Integer getMaxMethodLength() { return maxMethodLength; }
    public void setMaxMethodLength(Integer maxMethodLength) { this.maxMethodLength = maxMethodLength; }

    public Integer getMaxNestingDepth() { return maxNestingDepth; }
    public void setMaxNestingDepth(Integer maxNestingDepth) { this.maxNestingDepth = maxNestingDepth; }

    public Integer getImportCount() { return importCount; }
    public void setImportCount(Integer importCount) { this.importCount = importCount; }

    public Integer getCommitCount() { return commitCount; }
    public void setCommitCount(Integer commitCount) { this.commitCount = commitCount; }

    public Integer getChurnLines() { return churnLines; }
    public void setChurnLines(Integer churnLines) { this.churnLines = churnLines; }

    public Integer getDefectHistoryCount() { return defectHistoryCount; }
    public void setDefectHistoryCount(Integer defectHistoryCount) { this.defectHistoryCount = defectHistoryCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
