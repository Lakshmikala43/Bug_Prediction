# Measure Production Lines of Code (Excluding tests, tools, .git, and libraries)
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "   BugPredict AI - Production Code Metrics    " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$mainJavaFiles = Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java"
$resourceFiles = Get-ChildItem -Path "src\main\resources" -Recurse -File | Where-Object { $_.FullName -notmatch "user_cacerts" }
$testFiles = Get-ChildItem -Path "src\test\java" -Recurse -Filter "*.java" -ErrorAction SilentlyContinue

$javaLoc = 0
$javaSloc = 0
foreach ($f in $mainJavaFiles) {
    $lines = Get-Content $f.FullName
    $javaLoc += $lines.Count
    $javaSloc += ($lines | Where-Object { $_.Trim() -ne "" -and -not $_.Trim().StartsWith("//") -and -not $_.Trim().StartsWith("/*") -and -not $_.Trim().StartsWith("*") }).Count
}

$frontendLoc = 0
foreach ($f in $resourceFiles) {
    $lines = Get-Content $f.FullName -ErrorAction SilentlyContinue
    if ($lines) { $frontendLoc += $lines.Count }
}

$testLoc = 0
if ($testFiles) {
    foreach ($f in $testFiles) {
        $lines = Get-Content $f.FullName
        $testLoc += $lines.Count
    }
}

Write-Host "Java Production Files : $($mainJavaFiles.Count)" -ForegroundColor Green
Write-Host "Java Production LOC   : $javaLoc lines" -ForegroundColor Green
Write-Host "Java Production SLOC  : $javaSloc lines" -ForegroundColor Green
Write-Host "Frontend / Config LOC : $frontendLoc lines" -ForegroundColor Green
Write-Host "Test Suite LOC        : $testLoc lines" -ForegroundColor Yellow
Write-Host "---------------------------------------------"
Write-Host "Total Verified Lines  : $($javaLoc + $frontendLoc + $testLoc)" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
