#!/bin/bash

echo "Building TVG Digitizing Assistant v1.6.2 using IntelliJ build system..."
echo ""

# Check if we're in the right directory
if [ ! -f "DigitizingAssistant.iml" ]; then
    echo "Error: DigitizingAssistant.iml not found. Please run this script from the project root directory."
    exit 1
fi

# Check if target directory exists, if not create it
if [ ! -d "target" ]; then
    mkdir -p target
fi

# Check if classes directory exists, if not create it
if [ ! -d "target/classes" ]; then
    mkdir -p target/classes
fi

echo "Building project..."
echo ""

# Compile the project using javac
echo "Compiling Java sources..."
javac -cp ".:lib/*" -d target/classes src/com/thevideogoat/digitizingassistant/**/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    echo ""
    echo "Please ensure:"
    echo "1. Java 11+ is installed and in PATH"
    echo "2. All dependencies are in the lib/ directory"
    echo "3. You're running from the project root directory"
    exit 1
fi

echo "Compilation successful!"
echo ""

# Copy resources
echo "Copying resources..."
if [ -d "res" ]; then
    cp -r res/* target/classes/ 2>/dev/null || true
fi

# Create JAR file
echo "Creating JAR file..."
cd target/classes
jar cfm ../digitizing-assistant-1.6.1.jar ../../MANIFEST.MF com/ res/ 2>/dev/null
cd ../..

if [ $? -ne 0 ]; then
    echo "JAR creation failed!"
    exit 1
fi

echo ""
echo "Build successful!"
echo "JAR file created: target/digitizing-assistant-1.6.2.jar"
echo ""
echo "To run the application:"
echo "java -jar target/digitizing-assistant-1.6.2.jar"
echo ""
