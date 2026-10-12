package com.bugpredict.service.parser;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UniversalCodeScannerService {

    public static class CodeAnalysisResult {
        public String language = "Unknown";
        public int loc = 0;
        public int sloc = 0;
        public int cyclomaticComplexity = 1;
        public int maxNestingDepth = 0;
        public int maxMethodLength = 0;
        public int bugCount = 0;
        public List<DetectedBug> detectedBugs = new ArrayList<>();
        public String correctedCode = "";
    }

    public static class DetectedBug {
        public int lineNumber;
        public String ruleName;
        public String severity;
        public String message;
        public String recommendation;
        public String originalSnippet;
        public String suggestedPatch;

        public DetectedBug(int lineNumber, String ruleName, String severity, String message, String recommendation, String originalSnippet, String suggestedPatch) {
            this.lineNumber = lineNumber;
            this.ruleName = ruleName;
            this.severity = severity;
            this.message = message;
            this.recommendation = recommendation;
            this.originalSnippet = originalSnippet;
            this.suggestedPatch = suggestedPatch;
        }
    }

    public String detectLanguage(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".java")) return "Java";
        if (lower.endsWith(".py")) return "Python";
        if (lower.endsWith(".js")) return "JavaScript";
        if (lower.endsWith(".ts")) return "TypeScript";
        if (lower.endsWith(".c") || lower.endsWith(".h")) return "C";
        if (lower.endsWith(".cpp") || lower.endsWith(".hpp") || lower.endsWith(".cc")) return "C++";
        if (lower.endsWith(".cs")) return "C#";
        if (lower.endsWith(".go")) return "Go";
        if (lower.endsWith(".php")) return "PHP";
        if (lower.endsWith(".rb")) return "Ruby";
        if (lower.endsWith(".rs")) return "Rust";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "HTML";
        if (lower.endsWith(".css")) return "CSS";
        if (lower.endsWith(".sql")) return "SQL";
        return "Source";
    }

    public boolean isSupportedSourceFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".java") || lower.endsWith(".py") || lower.endsWith(".js") ||
               lower.endsWith(".ts") || lower.endsWith(".c") || lower.endsWith(".cpp") ||
               lower.endsWith(".cs") || lower.endsWith(".go") || lower.endsWith(".php") ||
               lower.endsWith(".rb") || lower.endsWith(".rs") || lower.endsWith(".sql");
    }

    public CodeAnalysisResult analyzeAndAutoCorrect(File file) {
        CodeAnalysisResult result = new CodeAnalysisResult();
        result.language = detectLanguage(file.getName());

        List<String> lines;
        try {
            lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            try {
                lines = Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1);
            } catch (Exception ex) {
                return result;
            }
        }

        result.loc = lines.size();
        List<String> correctedLines = new ArrayList<>(lines);

        int complexity = 1;
        int maxNesting = 0;
        int currentNesting = 0;
        int currentMethodLength = 0;
        int maxMethodLen = 0;

        Pattern branchPattern = Pattern.compile("\\b(if|for|while|case|catch|elif|except|switch)\\b");

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            int lineNum = i + 1;

            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                continue;
            }
            result.sloc++;

            if (branchPattern.matcher(trimmed).find()) {
                complexity++;
            }

            // Nesting depth approximation
            int openBraces = countOccurrences(trimmed, '{');
            int closeBraces = countOccurrences(trimmed, '}');
            currentNesting += (openBraces - closeBraces);
            if (currentNesting > maxNesting) maxNesting = currentNesting;
            if (currentNesting <= 0) currentNesting = 0;

            // Bug Pattern 1: Null Pointer / Undefined check omission (e.g. obj.method() without null verification)
            if (trimmed.contains(".equals(\"") || trimmed.matches(".*[a-zA-Z0-9_]+\\.equals\\(\".*\"\\).*")) {
                // e.g. str.equals("constant") should be "constant".equals(str) to prevent NPE
                result.detectedBugs.add(new DetectedBug(
                    lineNum, "NPE_RISK_YODA_CONDITION", "HIGH",
                    "String literal comparison on variable target creates NullPointerException risk.",
                    "Invert comparison to evaluate constant literal first.",
                    trimmed,
                    trimmed.replaceAll("([a-zA-Z0-9_]+)\\.equals\\((\"[^\"]+\")\\)", "$2.equals($1)")
                ));
                correctedLines.set(i, line.replaceAll("([a-zA-Z0-9_]+)\\.equals\\((\"[^\"]+\")\\)", "$2.equals($1)"));
            }

            // Bug Pattern 2: Empty Catch Block / Swallowed Exception
            if (trimmed.matches(".*catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}.*") || (trimmed.startsWith("catch") && trimmed.endsWith("{}"))) {
                result.detectedBugs.add(new DetectedBug(
                    lineNum, "EMPTY_EXCEPTION_BLOCK", "CRITICAL",
                    "Empty catch block swallows critical execution exceptions silently.",
                    "Log the error or rethrow standard exception.",
                    trimmed,
                    line.replace("{}", "{ logger.warn(\"Handled exception: \", e); }")
                ));
                correctedLines.set(i, line.replace("{}", "{ logger.warn(\"Handled exception: \", e); }"));
            }

            // Bug Pattern 3: Use of loose equality == in JavaScript/TypeScript
            if (("JavaScript".equals(result.language) || "TypeScript".equals(result.language)) && trimmed.contains(" == ") && !trimmed.contains(" === ")) {
                result.detectedBugs.add(new DetectedBug(
                    lineNum, "TYPE_COERCION_EQUALITY", "MEDIUM",
                    "Loose equality operator '==' causes unintended type coercion vulnerabilities.",
                    "Use strict equality '==='.",
                    trimmed,
                    trimmed.replace(" == ", " === ")
                ));
                correctedLines.set(i, line.replace(" == ", " === "));
            }

            // Bug Pattern 4: Python mutable default argument def foo(x=[])
            if ("Python".equals(result.language) && trimmed.matches(".*def\\s+[a-zA-Z0-9_]+\\([^)]*=\\s*\\[\\].*")) {
                result.detectedBugs.add(new DetectedBug(
                    lineNum, "MUTABLE_DEFAULT_ARG", "HIGH",
                    "Default argument value is mutable (list/dict), which persists state across function calls.",
                    "Use None as default and initialize inside function.",
                    trimmed,
                    trimmed.replace("=[]", "=None")
                ));
                correctedLines.set(i, line.replace("=[]", "=None"));
            }

            // Bug Pattern 5: Resource Leak (FileInputStream/Scanner opened without try-with-resources)
            if (trimmed.contains("new FileInputStream(") || trimmed.contains("new Scanner(System.in)")) {
                if (!trimmed.contains("try (") && !trimmed.startsWith("try(")) {
                    result.detectedBugs.add(new DetectedBug(
                        lineNum, "POTENTIAL_RESOURCE_LEAK", "HIGH",
                        "Unclosed I/O resource can lead to file descriptor exhaustion.",
                        "Encapsulate stream allocation in a try-with-resources statement.",
                        trimmed,
                        "// Suggestion: Wrap resource in try-with-resources block\n" + trimmed
                    ));
                }
            }

            // Bug Pattern 6: SQL Injection Vulnerability pattern
            if (trimmed.matches(".*(SELECT|INSERT|UPDATE|DELETE).*\\+.*[a-zA-Z0-9_]+.*") || trimmed.contains("executeQuery(\"SELECT \" +") || trimmed.contains("query(\"SELECT \" +")) {
                result.detectedBugs.add(new DetectedBug(
                    lineNum, "SQL_INJECTION_VULNERABILITY", "CRITICAL",
                    "Dynamic string concatenation inside SQL query introduces SQL Injection vulnerability.",
                    "Use parameterized PreparedStatements or ORM bind parameters.",
                    trimmed,
                    "// Refactored with parameterized query\n" + trimmed
                ));
            }
        }

        result.cyclomaticComplexity = complexity;
        result.maxNestingDepth = maxNesting;
        result.bugCount = result.detectedBugs.size();
        result.correctedCode = String.join(System.lineSeparator(), correctedLines);

        return result;
    }

    private int countOccurrences(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) count++;
        }
        return count;
    }
}
