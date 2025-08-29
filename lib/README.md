# Dependencies Directory

Place your JAR dependencies in this directory. The build scripts will automatically include them in the classpath.

## Required Dependencies

### Gson (JSON handling)
- **File**: `gson-2.10.1.jar`
- **Download**: https://mvnrepository.com/artifact/com.google.code.gson/gson/2.10.1
- **Purpose**: JSON serialization/deserialization

### VLCJ (Video processing - optional)
- **File**: `vlcj-4.8.2.jar`
- **Download**: https://mvnrepository.com/artifact/uk.co.caprica/vlcj/4.8.2
- **Purpose**: Video file processing (if needed)

## How to Add Dependencies

1. Download the JAR files from the links above
2. Place them in this `lib/` directory
3. Update the `MANIFEST.MF` file if you add new dependencies
4. Rebuild the project using the build scripts

## Alternative: Using IntelliJ's Build Artifacts

Instead of manual JAR creation, you can also use IntelliJ's built-in artifact system:

1. Go to **File → Project Structure**
2. Select **Artifacts**
3. Click **+** and choose **JAR → From modules with dependencies**
4. Select your main module
5. Configure the main class: `com.thevideogoat.digitizingassistant.ui.DigitizingAssistant`
6. Build using **Build → Build Artifacts**
