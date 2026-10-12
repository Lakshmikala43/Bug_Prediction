package com.bugpredict;

import com.bugpredict.model.FileMetric;
import com.bugpredict.model.PredictionResult;
import com.bugpredict.service.parser.JavaAstParserService;
import com.bugpredict.service.prediction.RiskPredictionService;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class BugPredictCoreTests {

    @Test
    public void testRiskPredictionLogic() {
        RiskPredictionService service = new RiskPredictionService();
        FileMetric metric = new FileMetric();
        metric.setCyclomaticComplexity(30);
        metric.setMaxNestingDepth(6);
        metric.setDefectHistoryCount(4);
        metric.setMaxMethodLength(90);
        metric.setChurnLines(400);

        RiskPredictionService.PredictionEvaluation eval = service.predictRisk(metric, 65.0, 85.0);
        assertNotNull(eval);
        assertTrue(eval.getRiskScore() >= 85.0, "Score should be critical risk");
        assertEquals(PredictionResult.RiskCategory.CRITICAL, eval.getCategory());
        assertTrue(eval.getExplanation().contains("Very high cyclomatic complexity"));
    }

    @Test
    public void testJavaAstParserServiceOnSelf() {
        JavaAstParserService parser = new JavaAstParserService();
        File sourceFile = new File("src/main/java/com/bugpredict/BugPredictApplication.java");
        assertTrue(sourceFile.exists(), "Source file must exist");

        JavaAstParserService.FileAstMetrics metrics = parser.parseJavaFile(sourceFile);
        assertFalse(metrics.parseError);
        assertEquals("com.bugpredict", metrics.packageName);
        assertTrue(metrics.loc > 0);
        assertTrue(metrics.classCount >= 1);
        assertTrue(metrics.methodCount >= 1);
    }
}
