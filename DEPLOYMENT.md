# TVG Digitizing Assistant - Deployment Guide

## Overview

This guide explains how to build and deploy the TVG Digitizing Assistant application for use on technician workstations using IntelliJ's build system.

## Prerequisites

### Required Software
- **Java 11 or higher** - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/)
- **IntelliJ IDEA** (for development) - Download from [JetBrains](https://www.jetbrains.com/idea/)

### Dependencies
- **Gson JAR** - Download from [Maven Central](https://mvnrepository.com/artifact/com.google.code.gson/gson/2.10.1)
- **VLCJ JAR** (optional) - Download from [Maven Central](https://mvnrepository.com/artifact/uk.co.caprica/vlcj/4.8.2)

### Verify Installation
```bash
# Check Java version
java -version

# Check javac version
javac -version
```

## Setting Up Dependencies

1. **Create lib directory** (if it doesn't exist):
   ```bash
   mkdir lib
   ```

2. **Download and add dependencies**:
   - Download `gson-2.10.1.jar` and place in `lib/` directory
   - Download `vlcj-4.8.2.jar` (if needed) and place in `lib/` directory

## Building the Application

### Option 1: Using Build Scripts (Recommended)

#### Windows
```cmd
# Run the build script
build.bat
```

#### Linux/Mac
```bash
# Make the script executable (first time only)
chmod +x build.sh

# Run the build script
./build.sh
```

### Option 2: Using IntelliJ's Build Artifacts (Recommended for Development)

1. **Open project in IntelliJ IDEA**
2. **Configure Artifact**:
   - Go to **File → Project Structure**
   - Select **Artifacts**
   - Click **+** and choose **JAR → From modules with dependencies**
   - Select your main module
   - Set main class: `com.thevideogoat.digitizingassistant.ui.DigitizingAssistant`
   - Click **OK**

3. **Build the artifact**:
   - Go to **Build → Build Artifacts**
   - Select your artifact and choose **Build**

### Option 3: Manual Build
```bash
# Compile
javac -cp ".:lib/*" -d target/classes src/com/thevideogoat/digitizingassistant/**/*.java

# Create JAR
jar cfm target/digitizing-assistant-1.6.1.jar MANIFEST.MF -C target/classes .
```

## Build Output

After a successful build, you'll find:
- **JAR File**: `target/digitizing-assistant-1.6.1.jar` (build scripts) or `out/artifacts/` (IntelliJ)
- **Executable**: Can be run directly with `java -jar`

## Deployment Options

### Option 1: Simple JAR Deployment (Recommended)

1. **Copy the JAR file** to each technician workstation
2. **Copy the lib directory** with dependencies
3. **Create a launcher script** for easy execution

#### Windows Launcher (`run.bat`)
```batch
@echo off
java -jar digitizing-assistant-1.6.1.jar
pause
```

#### Linux/Mac Launcher (`run.sh`)
```bash
#!/bin/bash
java -jar digitizing-assistant-1.6.1.jar
```

### Option 2: Desktop Shortcut

#### Windows
1. Right-click on desktop → New → Shortcut
2. Target: `java -jar "C:\path\to\digitizing-assistant-1.6.1.jar"`
3. Name: "TVG Digitizing Assistant"

#### Linux/Mac
1. Create desktop entry file
2. Make executable: `chmod +x digitizing-assistant-1.6.1.jar`

### Option 3: System Installation

#### Windows
1. Create installation directory: `C:\Program Files\TVG\DigitizingAssistant\`
2. Copy JAR file and lib directory
3. Create batch file in same directory
4. Add to PATH or create Start Menu shortcut

#### Linux
```bash
# Install to system directory
sudo mkdir -p /opt/tvg-digitizing-assistant
sudo cp digitizing-assistant-1.6.1.jar /opt/tvg-digitizing-assistant/
sudo cp -r lib/ /opt/tvg-digitizing-assistant/
sudo chmod +x /opt/tvg-digitizing-assistant/digitizing-assistant-1.6.1.jar

# Create desktop entry
sudo tee /usr/share/applications/tvg-digitizing-assistant.desktop << EOF
[Desktop Entry]
Name=TVG Digitizing Assistant
Comment=Video digitization project management
Exec=java -jar /opt/tvg-digitizing-assistant/digitizing-assistant-1.6.1.jar
Icon=/opt/tvg-digitizing-assistant/icon.png
Terminal=false
Type=Application
Categories=AudioVideo;
EOF
```

## Update Process

### For New Versions

1. **Build new version** using IntelliJ or build scripts
2. **Distribute new JAR file** to workstations
3. **Replace old JAR** with new version
4. **Test on one workstation** before full deployment

### Automated Updates (Future Enhancement)

Consider implementing:
- **Auto-update mechanism** within the application
- **GitHub releases** for easy distribution
- **Update checker** that notifies users of new versions

## Troubleshooting

### Common Issues

#### "Java not found"
- Ensure Java 11+ is installed
- Add Java to system PATH
- Verify with `java -version`

#### "JAR won't run"
- Check Java version compatibility
- Ensure JAR file is not corrupted
- Try running from command line for error messages

#### "Dependencies missing"
- Ensure all JAR files are in the `lib/` directory
- Check that MANIFEST.MF includes correct classpath
- Verify dependencies are accessible

### Build Issues

#### Compilation Errors
- Check Java version (requires Java 11+)
- Verify all source files are in correct locations
- Check that dependencies are in `lib/` directory

#### IntelliJ Build Issues
- Refresh project: **File → Reload All from Disk**
- Invalidate caches: **File → Invalidate Caches**
- Check module dependencies in **Project Structure**

## Security Considerations

### File Permissions
- Ensure JAR file has appropriate read permissions
- Consider digital signatures for distribution
- Verify file integrity after transfer

### Network Security
- Application runs locally (no network required)
- Project files stored in user's Documents folder
- No external connections made by application

## Performance Optimization

### JVM Options
For better performance, consider adding JVM options:
```bash
java -Xmx2g -Xms512m -jar digitizing-assistant-1.6.1.jar
```

### Memory Settings
- **Minimum**: 512MB RAM
- **Recommended**: 2GB+ RAM for large projects
- **Heap size**: Adjust based on project size

## Support

For deployment issues:
1. Check this guide first
2. Verify system requirements
3. Test on clean system
4. Contact development team with specific error messages

---

**Version**: 1.6.2  
**Last Updated**: December 2024
