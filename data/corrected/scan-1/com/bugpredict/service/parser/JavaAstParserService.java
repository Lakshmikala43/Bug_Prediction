package com.bugpredict.service.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

@Service
public class JavaAstParserService {

    public static class FileAstMetrics {
        public String packageName = "default";
        public int loc = 0;
        public int sloc = 0;
        public int classCount = 0;
        public int methodCount = 0;
        public int cyclomaticComplexity = 0;
        public int maxMethodLength = 0;
        public int maxNestingDepth = 0;
        public int importCount = 0;
        public boolean parseError = false;
        public String parseErrorMessage = "";
    }

    public FileAstMetrics parseJavaFile(File file) {
        FileAstMetrics metrics = new FileAstMetrics();
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            metrics.loc = lines.size();
            metrics.sloc = (int) lines.stream()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.startsWith("//") && !l.startsWith("/*") && !l.startsWith("*"))
                    .count();

            JavaParser javaParser = new JavaParser();
            ParseResult<CompilationUnit> result = javaParser.parse(file);

            if (result.isSuccessful() && result.getResult().isPresent()) {
                CompilationUnit cu = result.getResult().get();
                cu.getPackageDeclaration().ifPresent(pd -> metrics.packageName = pd.getNameAsString());
                metrics.importCount = cu.getImports().size();

                List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
                metrics.classCount = classes.size();

                List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
                metrics.methodCount = methods.size();

                int totalComplexity = 0;
                int maxMethodLen = 0;
                int maxNesting = 0;

                for (MethodDeclaration m : methods) {
                    if (m.getRange().isPresent()) {
                        int methodLen = m.getRange().get().end.line - m.getRange().get().begin.line + 1;
                        if (methodLen > maxMethodLen) maxMethodLen = methodLen;
                    }

                    int methodComplexity = 1; // base complexity
                    methodComplexity += m.findAll(IfStmt.class).size();
                    methodComplexity += m.findAll(ForStmt.class).size();
                    methodComplexity += m.findAll(ForEachStmt.class).size();
                    methodComplexity += m.findAll(WhileStmt.class).size();
                    methodComplexity += m.findAll(DoStmt.class).size();
                    methodComplexity += m.findAll(CatchClause.class).size();
                    methodComplexity += m.findAll(SwitchEntry.class).stream().filter(se -> !se.getLabels().isEmpty()).count();

                    totalComplexity += methodComplexity;

                    int nesting = calculateMaxNesting(m);
                    if (nesting > maxNesting) maxNesting = nesting;
                }

                metrics.cyclomaticComplexity = totalComplexity;
                metrics.maxMethodLength = maxMethodLen;
                metrics.maxNestingDepth = maxNesting;
            } else {
                metrics.parseError = true;
                metrics.parseErrorMessage = result.getProblems().isEmpty() ? "Unknown parse issue" : result.getProblems().get(0).getMessage();
                // Estimate fallback metrics from sloc
                metrics.cyclomaticComplexity = Math.max(1, metrics.sloc / 20);
            }
        } catch (IOException e) {
            metrics.parseError = true;
            metrics.parseErrorMessage = e.getMessage();
        }

        return metrics;
    }

    private int calculateMaxNesting(MethodDeclaration method) {
        if (!method.getBody().isPresent()) return 0;
        return computeNestingDepth(method.getBody().get(), 1);
    }

    private int computeNestingDepth(Statement statement, int currentDepth) {
        int max = currentDepth;
        if (statement.isBlockStmt()) {
            for (Statement child : statement.asBlockStmt().getStatements()) {
                if (child.isIfStmt() || child.isForStmt() || child.isForEachStmt() || child.isWhileStmt() || child.isDoStmt() || child.isTryStmt() || child.isSwitchStmt()) {
                    max = Math.max(max, computeNestingDepth(child, currentDepth + 1));
                }
            }
        } else if (statement.isIfStmt()) {
            IfStmt ifStmt = statement.asIfStmt();
            max = Math.max(max, computeNestingDepth(ifStmt.getThenStmt(), currentDepth + 1));
            if (ifStmt.getElseStmt().isPresent()) {
                max = Math.max(max, computeNestingDepth(ifStmt.getElseStmt().get(), currentDepth + 1));
            }
        }
        return max;
    }
}