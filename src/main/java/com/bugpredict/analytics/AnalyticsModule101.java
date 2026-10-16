package com.bugpredict.analytics;

/**
 * Analytics Module 101 - Bug detection analytics component.
 * Provides statistical analysis for defect prediction pipeline.
 */
public class AnalyticsModule101 {
    private static final int MODULE_ID = 101;
    private final String moduleName = "AnalyticsModule101";

    public double calculateRiskScore(int loc, int complexity) {
        return Math.min((loc * 0.0001) + (complexity * 0.05), 1.0);
    }

    public String getModuleName() { return moduleName; }
    public int getModuleId() { return MODULE_ID; }
    
    public boolean isHighRisk(double score) { return score > 0.7; }
}
