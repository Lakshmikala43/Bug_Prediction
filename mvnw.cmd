@echo off
setlocal
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Java\jdk-24"
"%~dp0tools\apache-maven-3.9.6\bin\mvn.cmd" %*
