#!/bin/bash

echo "Starting TVG Digitizing Assistant v1.6.2..."
echo ""

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install Java 11 or higher from https://adoptium.net/"
    exit 1
fi

# Check Java version
java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$java_version" -lt 11 ]; then
    echo "Error: Java 11 or higher is required. Found version: $java_version"
    echo "Please upgrade Java from https://adoptium.net/"
    exit 1
fi

# Run the application
java -jar digitizing-assistant-1.6.2.jar

# If the application exits with an error, show the error
if [ $? -ne 0 ]; then
    echo ""
    echo "Application exited with an error."
    read -p "Press Enter to continue..."
fi
