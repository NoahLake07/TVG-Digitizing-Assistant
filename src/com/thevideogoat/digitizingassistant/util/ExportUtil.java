package com.thevideogoat.digitizingassistant.util;

import com.thevideogoat.digitizingassistant.data.Conversion;
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
        try {
            String baseFileName = project.getName() + "_digitizing_sheet";
            
            if (includeCSV) {
                exportDigitizingSheetCSV(project, outputPath.resolve(baseFileName + ".csv"), exportType);
            }
            
            if (includeJSON) {
                exportDigitizingSheetJSON(project, outputPath.resolve(baseFileName + ".json"), exportType);
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
    
    private static void exportDigitizingSheetCSV(Project project, Path outputPath, ExportType exportType) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath.toFile()))) {
            // Write header
            if (exportType == ExportType.CLIENT) {
                writer.println("Tape Name,Type,Status,Notes,Date of Conversion");
            } else {
                writer.println("Tape Name,Type,Status,Notes,Technician Notes,Duration,Date of Conversion,Time of Conversion,Damage History");
            }
            
            // Write data
            for (Conversion conversion : project.getConversions()) {
                if (exportType == ExportType.CLIENT) {
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        escapeCSV(conversion.name),
                        escapeCSV(conversion.type.toString()),
                        escapeCSV(conversion.status.toString()),
                        escapeCSV(conversion.note),
                        escapeCSV(conversion.dateOfConversion.toString()));
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
            }
        }
    }
    
    private static void exportDigitizingSheetJSON(Project project, Path outputPath, ExportType exportType) throws IOException {
        JsonObject projectJson = new JsonObject();
        projectJson.addProperty("projectName", project.getName());
        projectJson.addProperty("exportType", exportType.toString());
        projectJson.addProperty("exportDate", java.time.LocalDateTime.now().toString());
        
        JsonArray conversionsArray = new JsonArray();
        for (Conversion conversion : project.getConversions()) {
            JsonObject conversionJson = new JsonObject();
            conversionJson.addProperty("name", conversion.name);
            conversionJson.addProperty("type", conversion.type.toString());
            conversionJson.addProperty("status", conversion.status.toString());
            conversionJson.addProperty("note", conversion.note);
            conversionJson.addProperty("dateOfConversion", conversion.dateOfConversion.toString());
            conversionJson.addProperty("timeOfConversion", conversion.timeOfConversion.toString());
            conversionJson.addProperty("duration", formatDuration(conversion.duration));
            
            if (exportType != ExportType.CLIENT) {
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
        exportFileMap(project, outputPath, includeChecksums, 10); // Default max depth of 10
    }
    
    public static void exportFileMap(Project project, Path outputPath, boolean includeChecksums, int maxDepth) {
        try {
            JsonObject fileMapJson = new JsonObject();
            fileMapJson.addProperty("projectName", project.getName());
            fileMapJson.addProperty("exportDate", java.time.LocalDateTime.now().toString());
            fileMapJson.addProperty("includeChecksums", includeChecksums);
            
            JsonArray filesArray = new JsonArray();
            
            for (Conversion conversion : project.getConversions()) {
                if (conversion.linkedFiles != null) {
                    for (var fileRef : conversion.linkedFiles) {
                        // Process the linked file/directory
                        processFileOrDirectory(fileRef, conversion, filesArray, includeChecksums, maxDepth);
                    }
                }
            }
            
            fileMapJson.add("files", filesArray);
            
            // Save as JSON
            try (FileWriter writer = new FileWriter(outputPath.toFile())) {
                new GsonBuilder().setPrettyPrinting().create().toJson(fileMapJson, writer);
            }
            
            JOptionPane.showMessageDialog(null, 
                "File map exported successfully to:\n" + outputPath.toString(),
                "Export Complete", 
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, 
                "Error exporting file map: " + e.getMessage(),
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
            Files.walk(dirPath, maxDepth)
                .filter(Files::isRegularFile) // Only include regular files, not directories
                .forEach(filePath -> addFileToArray(filePath, conversion, filesArray, includeChecksums));
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
}
