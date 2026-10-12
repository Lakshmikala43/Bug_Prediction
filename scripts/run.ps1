# Run BugPredict AI locally on Windows
$ErrorActionPreference = "Stop"
Write-Host "Starting BugPredict AI on http://localhost:8080..." -ForegroundColor Cyan

$truststorePath = (Get-Item "user_cacerts").FullName.Replace('\', '/')
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStore=""$truststorePath"" -Djavax.net.ssl.trustStorePassword=changeit"

.\mvnw.cmd spring-boot:run
