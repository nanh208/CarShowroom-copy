@echo off
title ARES Backend (Spring Boot - Port 8080)
cd /d "%~dp0\backend"
echo ====================================================
echo   DANG KHOI CHAY ARES BACKEND (SPRING BOOT - PORT 8080)
echo ====================================================
mvn spring-boot:run
pause
