@echo off
echo ====================================================
echo   KHOI DONG TOAN BO HE THONG ARES STORE
echo   1. Backend:  http://localhost:8080/
echo   2. Frontend: http://localhost:5173/
echo ====================================================
start "ARES Backend (Port 8080)" cmd /c "%~dp0RUN_BACKEND.bat"
timeout /t 5 /nobreak >nul
start "ARES Frontend (Port 5173)" cmd /c "%~dp0RUN_FRONTEND.bat"
echo He thong da khoi dong xong tren 2 cua so doc lap!
