@echo off
echo Building TVG Digitizing Assistant v1.6.2 using IntelliJ build system...
echo.

REM Check if we're in the right directory
if not exist "DigitizingAssistant.iml" (
    echo Error: DigitizingAssistant.iml not found. Please run this script from the project root directory.
    pause
    exit /b 1
)

REM Check if target directory exists, if not create it
if not exist "target" mkdir target

REM Check if classes directory exists, if not create it
if not exist "target\classes" mkdir target\classes

echo Building project...
echo.

REM Compile the project using javac
echo Compiling Java sources...
javac -cp ".;lib/*" -d target/classes src/com/thevideogoat/digitizingassistant/**/*.java

if errorlevel 1 (
    echo Compilation failed!
    echo.
    echo Please ensure:
    echo 1. Java 11+ is installed and in PATH
    echo 2. All dependencies are in the lib/ directory
    echo 3. You're running from the project root directory
    pause
    exit /b 1
)

echo Compilation successful!
echo.

REM Copy resources
echo Copying resources...
if exist "res" (
    xcopy /E /I /Y res target\classes\
)

REM Create JAR file
echo Creating JAR file...
cd target\classes
jar cfm ..\digitizing-assistant-1.6.1.jar ..\..\MANIFEST.MF com\ res\ 2>nul
cd ..\..

if errorlevel 1 (
    echo JAR creation failed!
    pause
    exit /b 1
)

echo.
echo Build successful!
echo JAR file created: target\digitizing-assistant-1.6.2.jar
echo.
echo To run the application:
echo java -jar target\digitizing-assistant-1.6.2.jar
echo.
pause
