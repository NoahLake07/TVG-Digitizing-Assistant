@echo off
echo Starting TVG Digitizing Assistant v1.6.2...
echo.

REM Check if Java is available
java -version >nul 2>&1
if errorlevel 1 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 11 or higher from https://adoptium.net/
    pause
    exit /b 1
)

REM Run the application
java -jar digitizing-assistant-1.6.2.jar

REM If the application exits with an error, pause to show the error
if errorlevel 1 (
    echo.
    echo Application exited with an error.
    pause
)
