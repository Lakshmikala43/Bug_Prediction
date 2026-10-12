package com.bugpredict.service;

import com.bugpredict.model.PredictionResult;
import com.bugpredict.model.Scan;
import com.bugpredict.repository.PredictionResultRepository;
import com.bugpredict.repository.ScanRepository;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.List;

@Service
public class ReportExportService {

    private final ScanRepository scanRepository;
    private final PredictionResultRepository predictionResultRepository;

    public ReportExportService(ScanRepository scanRepository, PredictionResultRepository predictionResultRepository) {
        this.scanRepository = scanRepository;
        this.predictionResultRepository = predictionResultRepository;
    }

    public String generateCsvReport(Long scanId) {
        Scan scan = scanRepository.findById(scanId)
                .orElseThrow(() -> new IllegalArgumentException("Scan not found: " + scanId));
        List<PredictionResult> results = predictionResultRepository.findByScanIdOrderByRiskScoreDesc(scanId);

        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(new String[]{"Project", scan.getProject().getName()});
            writer.writeNext(new String[]{"Scan ID", String.valueOf(scan.getId())});
            writer.writeNext(new String[]{"Scan Date", scan.getCreatedAt().toString()});
            writer.writeNext(new String[]{"Total Files", String.valueOf(scan.getTotalFiles())});
            writer.writeNext(new String[]{"Average Risk", String.valueOf(scan.getAvgRiskScore())});
            writer.writeNext(new String[]{});
            writer.writeNext(new String[]{"File Name", "Relative Path", "Risk Score", "Risk Category", "Confidence", "Factors"});

            for (PredictionResult r : results) {
                writer.writeNext(new String[]{
                        r.getProjectFile().getFileName(),
                        r.getProjectFile().getRelativePath(),
                        String.valueOf(r.getRiskScore()),
                        r.getRiskCategory().name(),
                        String.format("%.2f", r.getConfidenceScore()),
                        r.getExplanation()
                });
            }
        } catch (Exception ignored) {}
        return sw.toString();
    }
}
