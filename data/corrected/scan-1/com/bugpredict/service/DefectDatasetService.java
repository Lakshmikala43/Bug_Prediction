package com.bugpredict.service;

import com.bugpredict.model.DefectRecord;
import com.bugpredict.model.Project;
import com.bugpredict.repository.DefectRecordRepository;
import com.bugpredict.repository.ProjectRepository;
import com.opencsv.CSVReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DefectDatasetService {

    private final DefectRecordRepository defectRecordRepository;
    private final ProjectRepository projectRepository;

    public DefectDatasetService(DefectRecordRepository defectRecordRepository, ProjectRepository projectRepository) {
        this.defectRecordRepository = defectRecordRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public int importDefectsFromCsv(Long projectId, MultipartFile file) throws Exception {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        int importedCount = 0;
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] header = reader.readNext();
            if (header == null) return 0;

            int pathIdx = 0;
            int classIdx = 1;
            int defectIdx = 2;
            int severityIdx = 3;

            for (int i = 0; i < header.length; i++) {
                String h = header[i].trim().toLowerCase();
                if (h.contains("path") || h.contains("file")) pathIdx = i;
                else if (h.contains("class")) classIdx = i;
                else if (h.contains("defect") || h.contains("bug") || h.contains("id")) defectIdx = i;
                else if (h.contains("severity") || h.contains("priority")) severityIdx = i;
            }

            String[] row;
            List<DefectRecord> records = new ArrayList<>();
            while ((row = reader.readNext()) != null) {
                if (row.length <= pathIdx) continue;
                String filePath = row[pathIdx].trim().replace('\\', '/');
                if (filePath.isEmpty()) continue;

                DefectRecord record = new DefectRecord();
                record.setProject(project);
                record.setFilePath(filePath);
                record.setClassName(row.length > classIdx ? row[classIdx].trim() : "");
                record.setDefectId(row.length > defectIdx ? row[defectIdx].trim() : "DEF-" + System.currentTimeMillis());
                record.setSeverity(row.length > severityIdx ? row[severityIdx].trim() : "MAJOR");
                record.setIsBuggy(true);
                record.setReportedAt(LocalDateTime.now());
                records.add(record);
                importedCount++;
            }
            defectRecordRepository.saveAll(records);
        }
        return importedCount;
    }
}