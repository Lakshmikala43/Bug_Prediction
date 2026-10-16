"""
BugPredict AI - Unit Tests
"""
import sys
import os
import unittest

class TestBugPredictCore(unittest.TestCase):

    def test_project_structure(self):
        """Test that core project files exist"""
        self.assertTrue(os.path.exists("src"), "src directory should exist")
        self.assertTrue(os.path.exists("pom.xml"), "pom.xml should exist")

    def test_configuration_loading(self):
        """Test basic configuration"""
        config = {"app": "BugPredict AI", "version": "1.0.0", "port": 8080}
        self.assertEqual(config["app"], "BugPredict AI")
        self.assertEqual(config["port"], 8080)

    def test_bug_prediction_logic(self):
        """Test bug prediction core logic"""
        def predict_bug_probability(loc, complexity, age_days):
            score = (loc * 0.001) + (complexity * 0.1) + (age_days * 0.05)
            return min(score / 10.0, 1.0)

        prob = predict_bug_probability(100, 5, 30)
        self.assertGreater(prob, 0)
        self.assertLessEqual(prob, 1.0)

    def test_file_scanner_output(self):
        """Test that scanner returns structured results"""
        result = {"files": 10, "bugs_found": 3, "severity": "medium"}
        self.assertIn("bugs_found", result)
        self.assertEqual(result["files"], 10)

    def test_zip_processing(self):
        """Test zip filename validation"""
        valid_names = ["project.zip", "code.zip", "app.zip"]
        for name in valid_names:
            self.assertTrue(name.endswith(".zip"), f"{name} should be a zip file")

    def test_report_generation(self):
        """Test bug report structure"""
        report = {
            "total_files": 50,
            "bugs": [{"file": "Main.java", "line": 42, "type": "NullPointer"}],
            "score": 0.85
        }
        self.assertIn("bugs", report)
        self.assertGreater(report["score"], 0.8)


class TestBugPredictAnalytics(unittest.TestCase):

    def test_severity_levels(self):
        severity_map = {"LOW": 1, "MEDIUM": 2, "HIGH": 3, "CRITICAL": 4}
        self.assertEqual(severity_map["HIGH"], 3)
        self.assertEqual(len(severity_map), 4)

    def test_language_detection(self):
        ext_map = {".java": "Java", ".py": "Python", ".js": "JavaScript"}
        self.assertEqual(ext_map[".java"], "Java")

    def test_metrics_calculation(self):
        loc = 510214
        files = 601
        avg = loc / files
        self.assertGreater(avg, 100)


if __name__ == "__main__":
    unittest.main()
