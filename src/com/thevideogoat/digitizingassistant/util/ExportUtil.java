package com.thevideogoat.digitizingassistant.util;

import com.thevideogoat.digitizingassistant.data.Conversion;
import com.thevideogoat.digitizingassistant.data.ConversionStatus;
import com.thevideogoat.digitizingassistant.data.FileReference;
import com.thevideogoat.digitizingassistant.data.Project;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExportUtil {
    
    public enum ExportType {
        CLIENT("Client Version"),
        ARCHIVAL("Archival Version"),
        TECHNICIAN("Technician Version");
        
        private final String displayName;
        
        ExportType(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    public static void exportDigitizingSheet(Project project, Path outputPath, ExportType exportType, boolean includeCSV, boolean includeJSON) {
        exportDigitizingSheet(project, outputPath, exportType, includeCSV, includeJSON, false);
    }
    
    public static void exportDigitizingSheet(Project project, Path outputPath, ExportType exportType, boolean includeCSV, boolean includeJSON, boolean excludeCancelled) {
        try {
            String baseFileName = project.getName() + "_digitizing_sheet";
            
            // For client version, always generate HTML
            if (exportType == ExportType.CLIENT) {
                String htmlFileName = project.getName() + "_digitizing_sheet_client_return_sheet.html";
                exportDigitizingSheetHTML(project, outputPath.resolve(htmlFileName), exportType, excludeCancelled);
            }
            
            if (includeCSV) {
                exportDigitizingSheetCSV(project, outputPath.resolve(baseFileName + ".csv"), exportType, excludeCancelled);
            }
            
            if (includeJSON) {
                exportDigitizingSheetJSON(project, outputPath.resolve(baseFileName + ".json"), exportType, excludeCancelled);
            }
            
            JOptionPane.showMessageDialog(null, 
                "Digitizing sheet exported successfully to:\n" + outputPath.toString(),
                "Export Complete", 
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, 
                "Error exporting digitizing sheet: " + e.getMessage(),
                "Export Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void exportDigitizingSheetCSV(Project project, Path outputPath, ExportType exportType, boolean excludeCancelled) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath.toFile()))) {
            // Write header
            if (exportType == ExportType.CLIENT) {
                // Client paper sheet format
                writer.println("Conversion Note,#,Type,Status,Technician Notes,Logs");
            } else {
                writer.println("Tape Name,Type,Status,Notes,Technician Notes,Duration,Date of Conversion,Time of Conversion,Damage History");
            }
            
            // Write data
            int index = 1;
            for (Conversion conversion : project.getConversions()) {
                // Skip cancelled conversions if requested
                if (excludeCancelled && conversion.status == ConversionStatus.CANCELLED) {
                    continue;
                }
                if (exportType == ExportType.CLIENT) {
                    // Client paper sheet columns: Conversion Note | # | Type | Status | Technician Notes | Logs
                    String technicianNotes = (conversion.technicianNotes != null && !conversion.technicianNotes.trim().isEmpty())
                        ? conversion.technicianNotes : "-";

                    String logs = "-";
                    if (conversion.damageHistory != null && !conversion.damageHistory.isEmpty()) {
                        List<String> damageEvents = new ArrayList<>();
                        for (Conversion.DamageEvent event : conversion.damageHistory) {
                            damageEvents.add(event.description);
                        }
                        logs = String.join("; ", damageEvents);
                    }

                    writer.printf("\"%s\",%d,\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        escapeCSV(conversion.note != null ? conversion.note : ""),
                        index,
                        escapeCSV(conversion.type.toString()),
                        escapeCSV(conversion.status.toString()),
                        escapeCSV(technicianNotes),
                        escapeCSV(logs));
                } else {
                    // Include damage history for archival and technician versions
                    String damageHistory = "";
                    if (conversion.damageHistory != null && !conversion.damageHistory.isEmpty()) {
                        List<String> damageEvents = new ArrayList<>();
                        for (Conversion.DamageEvent event : conversion.damageHistory) {
                            damageEvents.add(String.format("%s: %s", 
                                event.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                                event.description));
                        }
                        damageHistory = String.join("; ", damageEvents);
                    }
                    
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        escapeCSV(conversion.name),
                        escapeCSV(conversion.type.toString()),
                        escapeCSV(conversion.status.toString()),
                        escapeCSV(conversion.note),
                        escapeCSV(conversion.technicianNotes),
                        escapeCSV(formatDuration(conversion.duration)),
                        escapeCSV(conversion.dateOfConversion.toString()),
                        escapeCSV(conversion.timeOfConversion.toString()),
                        escapeCSV(damageHistory));
                }
                index++;
            }
        }
    }
    
    private static void exportDigitizingSheetJSON(Project project, Path outputPath, ExportType exportType, boolean excludeCancelled) throws IOException {
        JsonObject projectJson = new JsonObject();
        projectJson.addProperty("projectName", project.getName());
        projectJson.addProperty("exportType", exportType.toString());
        projectJson.addProperty("exportDate", java.time.LocalDateTime.now().toString());
        
        JsonArray conversionsArray = new JsonArray();
        for (Conversion conversion : project.getConversions()) {
            // Skip cancelled conversions if requested
            if (excludeCancelled && conversion.status == ConversionStatus.CANCELLED) {
                continue;
            }
            JsonObject conversionJson = new JsonObject();
            
            if (exportType == ExportType.CLIENT) {
                // For client version, use conversion notes as primary label and include technician notes/damage logs
                conversionJson.addProperty("conversionNotes", conversion.note);
                conversionJson.addProperty("tapeName", conversion.name);
                conversionJson.addProperty("type", conversion.type.toString());
                conversionJson.addProperty("status", conversion.status.toString());
                conversionJson.addProperty("dateOfConversion", conversion.dateOfConversion.toString());
                
                // Include technician notes for irreversible damage
                if (conversion.status == ConversionStatus.DAMAGE_IRREVERSIBLE && 
                    conversion.technicianNotes != null && !conversion.technicianNotes.trim().isEmpty()) {
                    conversionJson.addProperty("technicianNotes", conversion.technicianNotes);
                }
                
                // Include damage history for client version
                if (conversion.damageHistory != null && !conversion.damageHistory.isEmpty()) {
                    JsonArray damageArray = new JsonArray();
                    for (Conversion.DamageEvent event : conversion.damageHistory) {
                        JsonObject damageJson = new JsonObject();
                        damageJson.addProperty("timestamp", event.timestamp.toString());
                        damageJson.addProperty("description", event.description);
                        if (event.technicianNotes != null && !event.technicianNotes.trim().isEmpty()) {
                            damageJson.addProperty("technicianNotes", event.technicianNotes);
                        }
                        damageArray.add(damageJson);
                    }
                    conversionJson.add("damageHistory", damageArray);
                }
            } else {
                // For archival and technician versions, use original structure
                conversionJson.addProperty("name", conversion.name);
                conversionJson.addProperty("type", conversion.type.toString());
                conversionJson.addProperty("status", conversion.status.toString());
                conversionJson.addProperty("note", conversion.note);
                conversionJson.addProperty("dateOfConversion", conversion.dateOfConversion.toString());
                conversionJson.addProperty("timeOfConversion", conversion.timeOfConversion.toString());
                conversionJson.addProperty("duration", formatDuration(conversion.duration));
                conversionJson.addProperty("technicianNotes", conversion.technicianNotes);
                conversionJson.addProperty("isDataOnly", conversion.isDataOnly);
                
                // Include damage history for archival and technician versions
                if (conversion.damageHistory != null && !conversion.damageHistory.isEmpty()) {
                    JsonArray damageArray = new JsonArray();
                    for (Conversion.DamageEvent event : conversion.damageHistory) {
                        JsonObject damageJson = new JsonObject();
                        damageJson.addProperty("timestamp", event.timestamp.toString());
                        damageJson.addProperty("description", event.description);
                        damageJson.addProperty("technicianNotes", event.technicianNotes);
                        damageArray.add(damageJson);
                    }
                    conversionJson.add("damageHistory", damageArray);
                }
                
                // Include linked files for technician version
                if (exportType == ExportType.TECHNICIAN && conversion.linkedFiles != null) {
                    JsonArray filesArray = new JsonArray();
                    for (var fileRef : conversion.linkedFiles) {
                        filesArray.add(fileRef.getPath());
                    }
                    conversionJson.add("linkedFiles", filesArray);
                }
            }
            
            conversionsArray.add(conversionJson);
        }
        
        projectJson.add("conversions", conversionsArray);
        
        try (FileWriter writer = new FileWriter(outputPath.toFile())) {
            new GsonBuilder().setPrettyPrinting().create().toJson(projectJson, writer);
        }
    }
    
    public static void exportFileMap(Project project, Path outputPath, boolean includeChecksums) {
        exportFileMap(project, outputPath, includeChecksums, 10, false); // Default max depth of 10, include cancelled
    }
    
    public static void exportFileMap(Project project, Path outputPath, boolean includeChecksums, int maxDepth) {
        exportFileMap(project, outputPath, includeChecksums, maxDepth, false); // Include cancelled by default
    }
    
    public static void exportFileMap(Project project, Path outputPath, boolean includeChecksums, int maxDepth, boolean excludeCancelled) {
        try {
            // Pre-validation: Check for potential issues
            List<String> validationErrors = new ArrayList<>();
            List<String> validationWarnings = new ArrayList<>();
            
            // Check if project has conversions
            if (project.getConversions().isEmpty()) {
                validationErrors.add("No conversions found in project");
            }
            
            // Check for conversions with linked files
            int conversionsWithFiles = 0;
            int totalFiles = 0;
            for (Conversion conversion : project.getConversions()) {
                // Skip cancelled conversions if requested
                if (excludeCancelled && conversion.status == ConversionStatus.CANCELLED) {
                    continue;
                }
                if (conversion.linkedFiles != null && !conversion.linkedFiles.isEmpty()) {
                    conversionsWithFiles++;
                    totalFiles += conversion.linkedFiles.size();
                }
            }
            
            if (conversionsWithFiles == 0) {
                validationWarnings.add("No conversions have linked files");
            }
            
            // Check for potentially problematic paths
            for (Conversion conversion : project.getConversions()) {
                // Skip cancelled conversions if requested
                if (excludeCancelled && conversion.status == ConversionStatus.CANCELLED) {
                    continue;
                }
                if (conversion.linkedFiles != null) {
                    for (FileReference fileRef : conversion.linkedFiles) {
                        try {
                            java.nio.file.Path path = Paths.get(fileRef.getPath());
                            if (Files.exists(path) && Files.isDirectory(path)) {
                                // Check if directory is accessible
                                try {
                                    Files.list(path).limit(1).count(); // Test directory access
                                } catch (Exception e) {
                                    validationWarnings.add("Directory not accessible: " + fileRef.getPath() + " (" + e.getMessage() + ")");
                                }
                            }
                        } catch (Exception e) {
                            validationWarnings.add("Invalid path: " + fileRef.getPath() + " (" + e.getMessage() + ")");
                        }
                    }
                }
            }
            
            // Show validation results
            if (!validationErrors.isEmpty() || !validationWarnings.isEmpty()) {
                StringBuilder message = new StringBuilder();
                if (!validationErrors.isEmpty()) {
                    message.append("Errors found:\n");
                    for (String error : validationErrors) {
                        message.append("• ").append(error).append("\n");
                    }
                    message.append("\n");
                }
                if (!validationWarnings.isEmpty()) {
                    message.append("Warnings:\n");
                    for (String warning : validationWarnings) {
                        message.append("• ").append(warning).append("\n");
                    }
                    message.append("\n");
                }
                message.append("Found ").append(conversionsWithFiles).append(" conversions with ").append(totalFiles).append(" total linked files.\n\n");
                message.append("Do you want to continue with the export?");
                
                int result = JOptionPane.showConfirmDialog(null, 
                    message.toString(),
                    "File Map Export Validation", 
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            // Show progress dialog
            JDialog progressDialog = new JDialog((java.awt.Frame) null, "Exporting File Map", true);
            progressDialog.setSize(400, 150);
            progressDialog.setLocationRelativeTo(null);
            progressDialog.setResizable(false);
            
            JPanel panel = new JPanel(new java.awt.BorderLayout(10, 10));
            panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            JLabel statusLabel = new JLabel("Preparing file map export...");
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            
            panel.add(statusLabel, java.awt.BorderLayout.NORTH);
            panel.add(progressBar, java.awt.BorderLayout.CENTER);
            
            progressDialog.add(panel);
            
            // Start export in background thread
            Thread exportThread = new Thread(() -> {
                try {
                    JsonObject fileMapJson = new JsonObject();
                    fileMapJson.addProperty("projectName", project.getName());
                    fileMapJson.addProperty("exportDate", java.time.LocalDateTime.now().toString());
                    fileMapJson.addProperty("includeChecksums", includeChecksums);
                    
                    JsonArray filesArray = new JsonArray();
                    
                    int totalConversions = project.getConversions().size();
                    int processedConversions = 0;
                    
                    for (Conversion conversion : project.getConversions()) {
                        // Skip cancelled conversions if requested
                        if (excludeCancelled && conversion.status == ConversionStatus.CANCELLED) {
                            continue;
                        }
                        // Update progress
                        final int current = processedConversions;
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Processing conversion " + (current + 1) + " of " + totalConversions + ": " + conversion.name);
                        });
                        
                        if (conversion.linkedFiles != null) {
                            for (var fileRef : conversion.linkedFiles) {
                                // Process the linked file/directory
                                processFileOrDirectory(fileRef, conversion, filesArray, includeChecksums, maxDepth);
                            }
                        }
                        
                        processedConversions++;
                    }
                    
                    // Update final status
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Saving file map...");
                    });
                    
                    fileMapJson.add("files", filesArray);
                    
                    // Save as JSON
                    try (FileWriter writer = new FileWriter(outputPath.toFile())) {
                        new GsonBuilder().setPrettyPrinting().create().toJson(fileMapJson, writer);
                    }
                    
                    // Close progress dialog and show success
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(null, 
                            "File map exported successfully to:\n" + outputPath.toString(),
                            "Export Complete", 
                            JOptionPane.INFORMATION_MESSAGE);
                    });
                    
                } catch (Exception e) {
                    // Close progress dialog and show error
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(null, 
                            "Error exporting file map: " + e.getMessage(),
                            "Export Error", 
                            JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
            
            exportThread.start();
            progressDialog.setVisible(true);
                
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, 
                "Error starting file map export: " + e.getMessage(),
                "Export Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void processFileOrDirectory(FileReference fileRef, Conversion conversion, JsonArray filesArray, boolean includeChecksums, int maxDepth) {
        try {
            java.nio.file.Path path = Paths.get(fileRef.getPath());
            
            if (Files.exists(path)) {
                if (Files.isDirectory(path)) {
                    // If it's a directory, explore all files within it
                    exploreDirectory(path, conversion, filesArray, includeChecksums, maxDepth);
                } else {
                    // If it's a file, process it directly
                    addFileToArray(path, conversion, filesArray, includeChecksums);
                }
            } else {
                // File/directory doesn't exist
                JsonObject fileJson = new JsonObject();
                fileJson.addProperty("tapeName", conversion.name);
                fileJson.addProperty("filePath", fileRef.getPath());
                fileJson.addProperty("conversionStatus", conversion.status.toString());
                fileJson.addProperty("error", "File not found");
                filesArray.add(fileJson);
            }
        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Error processing file/directory for conversion " + conversion.name + ": " + e.getMessage());
            e.printStackTrace();
            
            JsonObject fileJson = new JsonObject();
            fileJson.addProperty("tapeName", conversion.name);
            fileJson.addProperty("filePath", fileRef.getPath());
            fileJson.addProperty("conversionStatus", conversion.status.toString());
            fileJson.addProperty("error", "Could not process: " + e.getMessage());
            filesArray.add(fileJson);
        }
    }
    
    private static void exploreDirectory(java.nio.file.Path dirPath, Conversion conversion, JsonArray filesArray, boolean includeChecksums, int maxDepth) {
        try {
            // Use a more robust approach with timeout protection
            List<java.nio.file.Path> files = new ArrayList<>();
            
            // Collect files with timeout protection
            try {
                Files.walk(dirPath, maxDepth)
                    .filter(Files::isRegularFile)
                    .limit(10000) // Limit to prevent infinite loops
                    .forEach(files::add);
            } catch (Exception e) {
                // If Files.walk fails, try a simpler approach
                try {
                    Files.list(dirPath)
                        .filter(Files::isRegularFile)
                        .limit(1000)
                        .forEach(files::add);
                } catch (Exception e2) {
                    throw new IOException("Could not list directory contents: " + e2.getMessage());
                }
            }
            
            // Process collected files
            for (java.nio.file.Path filePath : files) {
                addFileToArray(filePath, conversion, filesArray, includeChecksums);
            }
            
            // Add a summary entry for the directory
            JsonObject dirJson = new JsonObject();
            dirJson.addProperty("tapeName", conversion.name);
            dirJson.addProperty("filePath", dirPath.toString());
            dirJson.addProperty("conversionStatus", conversion.status.toString());
            dirJson.addProperty("type", "directory");
            dirJson.addProperty("fileCount", files.size());
            dirJson.addProperty("note", "Directory contains " + files.size() + " files");
            filesArray.add(dirJson);
            
        } catch (Exception e) {
            JsonObject fileJson = new JsonObject();
            fileJson.addProperty("tapeName", conversion.name);
            fileJson.addProperty("filePath", dirPath.toString());
            fileJson.addProperty("conversionStatus", conversion.status.toString());
            fileJson.addProperty("error", "Could not explore directory: " + e.getMessage());
            filesArray.add(fileJson);
        }
    }
    
    private static void addFileToArray(java.nio.file.Path filePath, Conversion conversion, JsonArray filesArray, boolean includeChecksums) {
        try {
            JsonObject fileJson = new JsonObject();
            fileJson.addProperty("tapeName", conversion.name);
            fileJson.addProperty("filePath", filePath.toString());
            fileJson.addProperty("fileName", filePath.getFileName().toString());
            fileJson.addProperty("conversionStatus", conversion.status.toString());
            fileJson.addProperty("fileSize", Files.size(filePath));
            fileJson.addProperty("lastModified", Files.getLastModifiedTime(filePath).toString());
            
            if (includeChecksums) {
                try {
                    fileJson.addProperty("checksum", calculateChecksum(filePath));
                } catch (Exception e) {
                    fileJson.addProperty("checksumError", "Could not calculate checksum: " + e.getMessage());
                }
            }
            
            filesArray.add(fileJson);
        } catch (Exception e) {
            JsonObject fileJson = new JsonObject();
            fileJson.addProperty("tapeName", conversion.name);
            fileJson.addProperty("filePath", filePath.toString());
            fileJson.addProperty("conversionStatus", conversion.status.toString());
            fileJson.addProperty("error", "Could not read file: " + e.getMessage());
            filesArray.add(fileJson);
        }
    }
    
    private static String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
    
    private static String formatDuration(java.time.Duration duration) {
        if (duration == null) return "0:00:00";
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }
    
    private static String calculateChecksum(java.nio.file.Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : md5sum) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("MD5 algorithm not available", e);
        }
    }
    
    private static void exportDigitizingSheetHTML(Project project, Path outputPath, ExportType exportType, boolean excludeCancelled) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath.toFile()))) {
            // Write HTML header with modern styling
            writer.println("<!DOCTYPE html>");
            writer.println("<html lang=\"en\">");
            writer.println("<head>");
            writer.println("    <meta charset=\"UTF-8\">");
            writer.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            writer.println("    <title>Media Return Sheet - " + escapeHTML(project.getName()) + "</title>");
            writer.println("    <style>");
            writer.println("        body {");
            writer.println("            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;");
            writer.println("            line-height: 1.6;");
            writer.println("            color: #333;");
            writer.println("            max-width: 1200px;");
            writer.println("            margin: 0 auto;");
            writer.println("            padding: 20px;");
            writer.println("            background-color: #f9f9f9;");
            writer.println("        }");
            writer.println("        .header {");
            writer.println("            text-align: center;");
            writer.println("            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);");
            writer.println("            color: white;");
            writer.println("            padding: 30px;");
            writer.println("            border-radius: 10px;");
            writer.println("            margin-bottom: 30px;");
            writer.println("            box-shadow: 0 4px 6px rgba(0,0,0,0.1);");
            writer.println("        }");
            writer.println("        .header h1 {");
            writer.println("            margin: 0;");
            writer.println("            font-size: 2.5em;");
            writer.println("            font-weight: 300;");
            writer.println("        }");
            writer.println("        .header p {");
            writer.println("            margin: 10px 0 0 0;");
            writer.println("            font-size: 1.2em;");
            writer.println("            opacity: 0.9;");
            writer.println("        }");
            writer.println("        .summary {");
            writer.println("            background: white;");
            writer.println("            padding: 20px;");
            writer.println("            border-radius: 8px;");
            writer.println("            margin-bottom: 30px;");
            writer.println("            box-shadow: 0 2px 4px rgba(0,0,0,0.1);");
            writer.println("        }");
            writer.println("        .summary-grid {");
            writer.println("            display: grid;");
            writer.println("            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));");
            writer.println("            gap: 20px;");
            writer.println("            margin-top: 15px;");
            writer.println("        }");
            writer.println("        .summary-item {");
            writer.println("            text-align: center;");
            writer.println("            padding: 15px;");
            writer.println("            background: #f8f9fa;");
            writer.println("            border-radius: 6px;");
            writer.println("            border-left: 4px solid #667eea;");
            writer.println("        }");
            writer.println("        .summary-number {");
            writer.println("            font-size: 2em;");
            writer.println("            font-weight: bold;");
            writer.println("            color: #667eea;");
            writer.println("        }");
            writer.println("        .summary-label {");
            writer.println("            color: #666;");
            writer.println("            font-size: 0.9em;");
            writer.println("            text-transform: uppercase;");
            writer.println("            letter-spacing: 1px;");
            writer.println("        }");
            writer.println("        .conversions {");
            writer.println("            background: white;");
            writer.println("            border-radius: 8px;");
            writer.println("            overflow: hidden;");
            writer.println("            box-shadow: 0 2px 4px rgba(0,0,0,0.1);");
            writer.println("        }");
            writer.println("        .conversion-item {");
            writer.println("            border-bottom: 1px solid #eee;");
            writer.println("            padding: 20px;");
            writer.println("        }");
            writer.println("        .conversion-item:last-child {");
            writer.println("            border-bottom: none;");
            writer.println("        }");
            writer.println("        .conversion-header {");
            writer.println("            display: flex;");
            writer.println("            justify-content: space-between;");
            writer.println("            align-items: center;");
            writer.println("            margin-bottom: 15px;");
            writer.println("        }");
            writer.println("        .tape-name {");
            writer.println("            font-size: 1.4em;");
            writer.println("            font-weight: bold;");
            writer.println("            color: #2c3e50;");
            writer.println("        }");
            writer.println("        .status-badge {");
            writer.println("            padding: 6px 12px;");
            writer.println("            border-radius: 20px;");
            writer.println("            font-size: 0.85em;");
            writer.println("            font-weight: bold;");
            writer.println("            text-transform: uppercase;");
            writer.println("            letter-spacing: 1px;");
            writer.println("        }");
            writer.println("        .status-not-started { background: #6c757d; color: white; }");
            writer.println("        .status-in-progress { background: #fd7e14; color: white; }");
            writer.println("        .status-basic-editing { background: #007bff; color: white; }");
            writer.println("        .status-completed { background: #28a745; color: white; }");
            writer.println("        .status-damaged { background: #dc3545; color: white; }");
            writer.println("        .status-damage-fixed { background: #ffc107; color: #212529; }");
            writer.println("        .status-damage-irreversible { background: #6f42c1; color: white; }");
            writer.println("        .status-cancelled { background: #6c757d; color: white; }");
            writer.println("        .conversion-details {");
            writer.println("            display: grid;");
            writer.println("            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));");
            writer.println("            gap: 15px;");
            writer.println("        }");
            writer.println("        .detail-group {");
            writer.println("            background: #f8f9fa;");
            writer.println("            padding: 15px;");
            writer.println("            border-radius: 6px;");
            writer.println("        }");
            writer.println("        .detail-label {");
            writer.println("            font-weight: bold;");
            writer.println("            color: #495057;");
            writer.println("            margin-bottom: 5px;");
            writer.println("            font-size: 0.9em;");
            writer.println("            text-transform: uppercase;");
            writer.println("            letter-spacing: 0.5px;");
            writer.println("        }");
            writer.println("        .detail-value {");
            writer.println("            color: #212529;");
            writer.println("            font-size: 1em;");
            writer.println("        }");
            writer.println("        .damage-section {");
            writer.println("            background: #fff3cd;");
            writer.println("            border: 1px solid #ffeaa7;");
            writer.println("            border-radius: 6px;");
            writer.println("            padding: 15px;");
            writer.println("            margin-top: 15px;");
            writer.println("        }");
            writer.println("        .damage-section h4 {");
            writer.println("            margin: 0 0 10px 0;");
            writer.println("            color: #856404;");
            writer.println("            font-size: 1.1em;");
            writer.println("        }");
            writer.println("        .damage-event {");
            writer.println("            background: white;");
            writer.println("            padding: 10px;");
            writer.println("            border-radius: 4px;");
            writer.println("            margin-bottom: 10px;");
            writer.println("            border-left: 3px solid #ffc107;");
            writer.println("        }");
            writer.println("        .damage-timestamp {");
            writer.println("            font-size: 0.85em;");
            writer.println("            color: #6c757d;");
            writer.println("            margin-bottom: 5px;");
            writer.println("        }");
            writer.println("        .damage-description {");
            writer.println("            font-weight: bold;");
            writer.println("            color: #856404;");
            writer.println("            margin-bottom: 5px;");
            writer.println("        }");
            writer.println("        .damage-notes {");
            writer.println("            color: #495057;");
            writer.println("            font-style: italic;");
            writer.println("        }");
            writer.println("        .footer {");
            writer.println("            text-align: center;");
            writer.println("            margin-top: 40px;");
            writer.println("            padding: 20px;");
            writer.println("            color: #6c757d;");
            writer.println("            font-size: 0.9em;");
            writer.println("        }");
            writer.println("        @media print {");
            writer.println("            body { background: white; }");
            writer.println("            .header { background: #667eea !important; -webkit-print-color-adjust: exact; }");
            writer.println("            .conversion-item { page-break-inside: avoid; }");
            writer.println("        }");
            writer.println("    </style>");
            writer.println("</head>");
            writer.println("<body>");
            
            // Header
            writer.println("    <div class=\"header\">");
            writer.println("        <h1>Media Return Sheet</h1>");
            writer.println("        <p>Project: " + escapeHTML(project.getName()) + "</p>");
            writer.println("        <p>Generated: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' h:mm a")) + "</p>");
            writer.println("    </div>");
            
            // Summary section
            writer.println("    <div class=\"summary\">");
            writer.println("        <h2>Project Summary</h2>");
            writer.println("        <div class=\"summary-grid\">");
            
            int totalConversions = project.getConversions().size();
            int completedConversions = 0;
            int damagedConversions = 0;
            int irreversibleDamage = 0;
            
            for (Conversion conversion : project.getConversions()) {
                if (conversion.status == ConversionStatus.COMPLETED) {
                    completedConversions++;
                }
                if (conversion.status == ConversionStatus.DAMAGED || 
                    conversion.status == ConversionStatus.DAMAGE_FIXED || 
                    conversion.status == ConversionStatus.DAMAGE_IRREVERSIBLE) {
                    damagedConversions++;
                }
                if (conversion.status == ConversionStatus.DAMAGE_IRREVERSIBLE) {
                    irreversibleDamage++;
                }
            }
            
            writer.println("            <div class=\"summary-item\">");
            writer.println("                <div class=\"summary-number\">" + totalConversions + "</div>");
            writer.println("                <div class=\"summary-label\">Total Media Items</div>");
            writer.println("            </div>");
            writer.println("            <div class=\"summary-item\">");
            writer.println("                <div class=\"summary-number\">" + completedConversions + "</div>");
            writer.println("                <div class=\"summary-label\">Successfully Converted</div>");
            writer.println("            </div>");
            writer.println("            <div class=\"summary-item\">");
            writer.println("                <div class=\"summary-number\">" + damagedConversions + "</div>");
            writer.println("                <div class=\"summary-label\">Items with Damage</div>");
            writer.println("            </div>");
            if (irreversibleDamage > 0) {
                writer.println("            <div class=\"summary-item\">");
                writer.println("                <div class=\"summary-number\">" + irreversibleDamage + "</div>");
                writer.println("                <div class=\"summary-label\">Irreversible Damage</div>");
                writer.println("            </div>");
            }
            writer.println("        </div>");
            writer.println("    </div>");
            
            // Conversions section
            writer.println("    <div class=\"conversions\">");
            writer.println("        <h2 style=\"margin: 0; padding: 20px 20px 0 20px; color: #2c3e50;\">Media Items</h2>");
            
            for (Conversion conversion : project.getConversions()) {
                // Skip cancelled conversions if requested
                if (excludeCancelled && conversion.status == ConversionStatus.CANCELLED) {
                    continue;
                }
                writer.println("        <div class=\"conversion-item\">");
                
                // Header with tape name and status
                writer.println("            <div class=\"conversion-header\">");
                writer.println("                <div class=\"tape-name\">" + escapeHTML(conversion.name) + "</div>");
                writer.println("                <div class=\"status-badge status-" + getStatusClass(conversion.status) + "\">" + 
                    escapeHTML(conversion.status.toString()) + "</div>");
                writer.println("            </div>");
                
                // Conversion details
                writer.println("            <div class=\"conversion-details\">");
                
                // Basic info
                writer.println("                <div class=\"detail-group\">");
                writer.println("                    <div class=\"detail-label\">Media Type</div>");
                writer.println("                    <div class=\"detail-value\">" + escapeHTML(conversion.type.toString()) + "</div>");
                writer.println("                </div>");
                
                writer.println("                <div class=\"detail-group\">");
                writer.println("                    <div class=\"detail-label\">Date of Conversion</div>");
                writer.println("                    <div class=\"detail-value\">" + escapeHTML(conversion.dateOfConversion.toString()) + "</div>");
                writer.println("                </div>");
                
                if (conversion.timeOfConversion != null) {
                    writer.println("                <div class=\"detail-group\">");
                    writer.println("                    <div class=\"detail-label\">Time of Conversion</div>");
                    writer.println("                    <div class=\"detail-value\">" + escapeHTML(conversion.timeOfConversion.toString()) + "</div>");
                    writer.println("                </div>");
                }
                
                if (conversion.duration != null && !conversion.duration.isZero()) {
                    writer.println("                <div class=\"detail-group\">");
                    writer.println("                    <div class=\"detail-label\">Duration</div>");
                    writer.println("                    <div class=\"detail-value\">" + formatDuration(conversion.duration) + "</div>");
                    writer.println("                </div>");
                }
                
                if (conversion.note != null && !conversion.note.trim().isEmpty()) {
                    writer.println("                <div class=\"detail-group\">");
                    writer.println("                    <div class=\"detail-label\">Client Notes</div>");
                    writer.println("                    <div class=\"detail-value\">" + escapeHTML(conversion.note) + "</div>");
                    writer.println("                </div>");
                }
                
                // Show technician notes for irreversible damage
                if (conversion.status == ConversionStatus.DAMAGE_IRREVERSIBLE && 
                    conversion.technicianNotes != null && !conversion.technicianNotes.trim().isEmpty()) {
                    writer.println("                <div class=\"detail-group\">");
                    writer.println("                    <div class=\"detail-label\">Technician Notes</div>");
                    writer.println("                    <div class=\"detail-value\">" + escapeHTML(conversion.technicianNotes) + "</div>");
                    writer.println("                </div>");
                }
                
                writer.println("            </div>");
                
                // Damage history section (if any damage events exist)
                if (conversion.damageHistory != null && !conversion.damageHistory.isEmpty()) {
                    writer.println("            <div class=\"damage-section\">");
                    writer.println("                <h4>Damage History</h4>");
                    
                    for (Conversion.DamageEvent event : conversion.damageHistory) {
                        writer.println("                <div class=\"damage-event\">");
                        writer.println("                    <div class=\"damage-timestamp\">" + 
                            event.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a")) + "</div>");
                        writer.println("                    <div class=\"damage-description\">" + escapeHTML(event.description) + "</div>");
                        if (event.technicianNotes != null && !event.technicianNotes.trim().isEmpty()) {
                            writer.println("                    <div class=\"damage-notes\">" + escapeHTML(event.technicianNotes) + "</div>");
                        }
                        writer.println("                </div>");
                    }
                    
                    writer.println("            </div>");
                }
                
                writer.println("        </div>");
            }
            
            writer.println("    </div>");
            
            // Footer
            writer.println("    <div class=\"footer\">");
            writer.println("        <p>This report was generated automatically by the Digitizing Assistant system.</p>");
            writer.println("        <p>For questions about your media conversion, please contact our support team.</p>");
            writer.println("    </div>");
            
            writer.println("</body>");
            writer.println("</html>");
        }
    }
    
    private static String getStatusClass(ConversionStatus status) {
        switch (status) {
            case NOT_STARTED: return "not-started";
            case IN_PROGRESS: return "in-progress";
            case BASIC_EDITING: return "basic-editing";
            case COMPLETED: return "completed";
            case DAMAGED: return "damaged";
            case DAMAGE_FIXED: return "damage-fixed";
            case DAMAGE_IRREVERSIBLE: return "damage-irreversible";
            case CANCELLED: return "cancelled";
            default: return "not-started";
        }
    }
    
    private static String escapeHTML(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
