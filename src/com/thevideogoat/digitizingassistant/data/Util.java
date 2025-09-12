package com.thevideogoat.digitizingassistant.data;

import com.thevideogoat.digitizingassistant.ui.DigitizingAssistant;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ListIterator;

public class Util {

    /**
     * Renames a file with proper error handling and validation.
     * 
     * @param file The file to rename
     * @param newName The new name for the file
     * @return The new file if rename succeeded, original file if it failed
     * @throws IllegalArgumentException if file is null or newName is empty
     */
    public static File renameFile(File file, String newName) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("New name cannot be null or empty");
        }
        
        // Validate that the file exists
        if (!file.exists()) {
            System.err.println("Warning: Cannot rename non-existent file: " + file.getAbsolutePath());
            return file;
        }
        
        // Validate that the file is writable
        if (!file.canWrite()) {
            System.err.println("Warning: Cannot rename read-only file: " + file.getAbsolutePath());
            return file;
        }
        
        String extension = "";
        int i = file.getName().lastIndexOf('.');
        if (i > 0) {
            extension = file.getName().substring(i);
        }
        
        File newFile;
        // Check if newName already contains the extension
        if (newName.toLowerCase().endsWith(extension.toLowerCase())) {
            // newName already has the extension, don't add it again
            newFile = new File(file.getParentFile(), newName);
        } else {
            // Add the extension to newName
            newFile = new File(file.getParentFile(), newName + extension);
        }
        
        // Check if target already exists and is different from source
        if (newFile.exists() && !newFile.equals(file)) {
            System.err.println("Warning: Target file already exists: " + newFile.getAbsolutePath());
            return file;
        }
        
        // Attempt the rename
        boolean success = file.renameTo(newFile);
        if (!success) {
            System.err.println("Error: Failed to rename file from " + file.getAbsolutePath() + " to " + newFile.getAbsolutePath());
            return file;
        }
        
        return newFile;
    }

    public static boolean deleteFile(File file){
        return file.delete();
    }

    public static String getProjectQueuePath(Project project){
        return DigitizingAssistant.PROJECTS_DIRECTORY + File.separator + project.getName() +".queue";
    }

    public static void renameLinkedFiles(Conversion c){
        if(c.linkedFiles.isEmpty()){
            return;
        }

        ListIterator<FileReference> iterator = c.linkedFiles.listIterator();
        int i = 0;
        while(iterator.hasNext()){
            FileReference fileRef = iterator.next();
            ++i;
            // Rename the file and replace it in the list
            File renamedFile = renameFile(fileRef.getFile(), c.name + (i > 1 ? " (" + i + ")" : ""));
            iterator.set(new FileReference(renamedFile));
        }

        // Completion dialog
        JOptionPane.showMessageDialog(null, "Renamed " + i + " files.", "Rename Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void renameLinkedFilesToNote(Conversion c){
        if(c.linkedFiles.isEmpty()){
            return;
        }

        ListIterator<FileReference> iterator = c.linkedFiles.listIterator();
        int i = 0;
        while(iterator.hasNext()){
            FileReference fileRef = iterator.next();
            ++i;
            // Rename the file and replace it in the list
            File renamedFile = renameFile(fileRef.getFile(), c.note + (i > 1 ? " (" + i + ")" : ""));
            iterator.set(new FileReference(renamedFile));
        }

        // Completion dialog
        JOptionPane.showMessageDialog(null, "Renamed " + i + " files.", "Rename Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void renameFiles(ArrayList<File> files, String newName){
        if(files.isEmpty()){
            return;
        }

        int i = 0;
        for (File f : files){
            renameFile(f, newName + (i > 0 ? " (" + i + ")" : ""));
            i++;
        }
        JOptionPane.showMessageDialog(null, "Renamed " + i + " files.", "Rename Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void relinkFiles(Project project) {
        // Prompt the user to select a new directory
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        // Set the current directory to the last used directory
        String lastDir = Preferences.getInstance().getLastUsedDirectory();
        if (lastDir != null && new File(lastDir).exists()) {
            fileChooser.setCurrentDirectory(new File(lastDir));
        }
        
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File newDirectory = fileChooser.getSelectedFile();
            
            // Save the selected directory as the last used directory
            Preferences.getInstance().setLastUsedDirectory(newDirectory.getAbsolutePath());

            // For each conversion in the project
            int i = 0, f = 0;
            for (Conversion conversion : project.getConversions()) {
                ArrayList<FileReference> updatedFiles = new ArrayList<>();

                // For each linked file in the conversion
                for (FileReference oldFile : conversion.linkedFiles) {
                    File newFile = new File(newDirectory, oldFile.getName());

                    // If a file with the same name is found in the new directory
                    if (newFile.exists()) {
                        updatedFiles.add(new FileReference(newFile));
                        i++;
                    } else {
                        // If not found, keep the old file reference
                        updatedFiles.add(oldFile);
                        f++;
                    }
                }

                // Update the conversion's linked files
                conversion.linkedFiles = updatedFiles;
            }

            // Notify the user of completion
            JOptionPane.showMessageDialog(null, i+" files relinked successfully.", "Relink Success", JOptionPane.INFORMATION_MESSAGE);
            if(f>0) JOptionPane.showMessageDialog(null, f+" files couldn't be relinked.", "Relink Failure", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static ArrayList<Conversion> sortConversionsBy(ArrayList<Conversion> conversions, String criteria) {
        ArrayList<Conversion> sorted = new ArrayList<>(conversions);
        
        switch (criteria.toLowerCase()) {
            case "name":
                sorted.sort((c1, c2) -> c1.name.compareToIgnoreCase(c2.name));
                break;
            case "natural sort":
                sorted.sort((c1, c2) -> naturalCompare(c1.name, c2.name));
                break;
            case "status":
                sorted.sort(Comparator.comparing(c -> c.status));
                break;
            case "duration":
                sorted.sort(Comparator.comparing(c -> c.duration));
                break;
            case "type":
                sorted.sort(Comparator.comparing(c -> c.type));
                break;
        }
        
        return sorted;
    }

    private static int naturalCompare(String a, String b) {
        if (a == null || b == null) {
            return 0;
        }
        
        int aLen = a.length();
        int bLen = b.length();
        int i = 0;
        int j = 0;
        
        while (i < aLen && j < bLen) {
            char aChar = a.charAt(i);
            char bChar = b.charAt(j);
            
            if (Character.isDigit(aChar) && Character.isDigit(bChar)) {
                // Extract complete numbers from both strings
                StringBuilder aNum = new StringBuilder();
                StringBuilder bNum = new StringBuilder();
                
                while (i < aLen && Character.isDigit(a.charAt(i))) {
                    aNum.append(a.charAt(i));
                    i++;
                }
                
                while (j < bLen && Character.isDigit(b.charAt(j))) {
                    bNum.append(b.charAt(j));
                    j++;
                }
                
                // Compare the numbers
                int numCompare = Integer.compare(
                    Integer.parseInt(aNum.toString()),
                    Integer.parseInt(bNum.toString())
                );
                
                if (numCompare != 0) {
                    return numCompare;
                }
            } else {
                // Compare characters case-insensitively
                int charCompare = Character.toLowerCase(aChar) - 
                                Character.toLowerCase(bChar);
                if (charCompare != 0) {
                    return charCompare;
                }
                i++;
                j++;
            }
        }
        
        // If we've reached here, one string might be longer than the other
        return aLen - bLen;
    }

    public static boolean isVideoFile(File file){
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mov") || name.endsWith(".mkv") || name.endsWith(".flv") || name.endsWith(".wmv");
    }

    public static ArrayList<FileReference> getLinkedFiles(Project p){
        ArrayList<FileReference> linkedFiles = new ArrayList<>();
        for (Conversion conversion : p.conversions) {
            linkedFiles.addAll(conversion.linkedFiles);
        }
        return linkedFiles;
    }

    public static ArrayList<FileReference> getVideoFiles(Project p){
        ArrayList<FileReference> videoFiles = new ArrayList<>();
        for (FileReference fileRef : getLinkedFiles(p)) {
            if (Util.isVideoFile(fileRef.getFile())) {
                videoFiles.add(fileRef);
            }
        }
        return videoFiles;
    }

    /**
     * Renames files with basic options and proper error handling.
     * 
     * @param files List of files to rename
     * @param newName New name for the files
     * @param includeSubdirectories Whether to include subdirectories
     * @param preserveNumbering Whether to preserve existing numbers
     * @return Number of files successfully renamed
     */
    public static int renameFilesWithOptions(ArrayList<File> files, String newName, 
        boolean includeSubdirectories, boolean preserveNumbering) {
        
        if (files == null || files.isEmpty()) {
            return 0;
        }

        int renamedCount = 0;
        int errorCount = 0;
        
        for (File file : files) {
            try {
                if (file.isDirectory() && includeSubdirectories) {
                    // Rename all files in directory and subdirectories
                    int subdirRenamed = renameFilesInDirectory(file, newName, includeSubdirectories, preserveNumbering);
                    renamedCount += subdirRenamed;
                } else {
                    // Rename just this file
                    String finalName = preserveNumbering ? 
                        preserveNumberInFilename(file.getName(), newName) : 
                        newName;
                    
                    File renamedFile = renameFile(file, finalName);
                    if (renamedFile != file) {
                        renamedCount++;
                    } else {
                        errorCount++;
                        System.err.println("Failed to rename file: " + file.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                errorCount++;
                System.err.println("Error renaming file " + file.getAbsolutePath() + ": " + e.getMessage());
            }
        }

        // Show completion message with error count if any
        String message = "Renamed " + renamedCount + " files/directories.";
        if (errorCount > 0) {
            message += "\n" + errorCount + " files could not be renamed (check console for details).";
        }
        
        JOptionPane.showMessageDialog(null, message, "Rename Complete", 
            errorCount > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
        
        return renamedCount;
    }
    
    /**
     * Renames files from FileReference list with proper reference updates.
     * 
     * @param fileRefs List of FileReference objects to rename
     * @param newName New name for the files
     * @param includeSubdirectories Whether to include subdirectories
     * @param preserveNumbering Whether to preserve existing numbers
     * @param conversion The conversion object for reference updates
     * @return Number of files successfully renamed
     */
    public static int renameFilesWithOptionsFromReferences(ArrayList<FileReference> fileRefs, String newName, 
        boolean includeSubdirectories, boolean preserveNumbering, Conversion conversion) {
        
        if (fileRefs == null || fileRefs.isEmpty()) {
            return 0;
        }

        // Convert FileReferences to Files and track the mapping
        ArrayList<File> files = new ArrayList<>();
        java.util.Map<File, FileReference> fileToRefMap = new java.util.HashMap<>();
        
        for (FileReference fileRef : fileRefs) {
            File file = fileRef.getFile();
            files.add(file);
            fileToRefMap.put(file, fileRef);
        }
        
        // Call the original method
        int renamedCount = renameFilesWithOptions(files, newName, includeSubdirectories, preserveNumbering);
        
        // Update FileReference objects to point to renamed files
        if (renamedCount > 0 && conversion != null) {
            // Use the new batch update method to handle reference updates
            for (FileReference fileRef : fileRefs) {
                File oldFile = fileRef.getFile();
                File parentDir = oldFile.getParentFile();
                if (parentDir != null && parentDir.exists()) {
                    updateLinkedFileReferencesAfterBatchRename(conversion, newName, parentDir);
                }
            }
        }
        
        return renamedCount;
    }
    
    /**
     * Legacy method for backward compatibility - calls the new method without conversion parameter.
     */
    public static void renameFilesWithOptionsFromReferences(ArrayList<FileReference> fileRefs, String newName, 
        boolean includeSubdirectories, boolean preserveNumbering) {
        
        renameFilesWithOptionsFromReferences(fileRefs, newName, includeSubdirectories, preserveNumbering, null);
    }

    private static String preserveNumberInFilename(String oldName, String newName) {
        // Extract any numbers from the old filename
        String numbers = oldName.replaceAll("[^0-9]", "");
        if (!numbers.isEmpty()) {
            // If old name had numbers, append them to new name
            return newName + "_" + numbers;
        }
        return newName;
    }

    private static int renameFilesInDirectory(File directory, String baseName, boolean includeSubdirectories, boolean preserveNumbering) {
        File[] files = directory.listFiles();
        if (files == null) return 0;

        int count = 0;
        int fileIndex = 0;

        for (File file : files) {
            if (file.isDirectory() && includeSubdirectories) {
                count += renameFilesInDirectory(file, baseName + "_" + file.getName(), includeSubdirectories, preserveNumbering);
            } else {
                String newName = baseName + (fileIndex > 0 ? " (" + fileIndex + ")" : "");
                String finalName = preserveNumbering ? 
                    preserveNumberInFilename(file.getName(), newName) : 
                    newName;
                renameFile(file, finalName);
                fileIndex++;
                count++;
            }
        }
        return count;
    }

    /**
     * Renames files with advanced options and proper error handling.
     * 
     * @param files List of files to rename
     * @param conversionName Name of the conversion
     * @param conversionNote Note for the conversion
     * @param separator Separator to use in filenames
     * @param addDate Whether to add date prefix
     * @param prefixName Whether to prefix with conversion name
     * @param prefixNote Whether to prefix with conversion note
     * @param suffixName Whether to suffix with conversion name
     * @param suffixNote Whether to suffix with conversion note
     * @param replace Whether to replace filename
     * @param smartReplace Whether to use smart replace
     * @param custom Whether to use custom format
     * @param customFormat Custom format string
     * @param includeSubdirectories Whether to include subdirectories
     * @param useSequential Whether to use sequential numbering
     * @param conversion The conversion object for reference updates
     * @return Number of files successfully renamed
     */
    public static int renameFilesWithAdvancedOptions(ArrayList<File> files, String conversionName, String conversionNote,
            String separator, boolean addDate, boolean prefixName, boolean prefixNote, 
            boolean suffixName, boolean suffixNote, boolean replace, boolean smartReplace, 
            boolean custom, String customFormat, boolean includeSubdirectories, boolean useSequential,
            Conversion conversion) {
        
        if (files == null || files.isEmpty()) {
            return 0;
        }

        int renamedCount = 0;
        int errorCount = 0;
        String datePrefix = addDate ? java.time.LocalDate.now().toString() + separator : "";

        // Process each file individually and handle conflicts as they come
        for (File file : files) {
            try {
                if (file.isDirectory()) {
                    if (includeSubdirectories) {
                        // Rename files INSIDE the directory (keep directory name)
                        int subdirRenamed = renameFilesInDirectoryAdvanced(file, conversionName, conversionNote, 
                            separator, datePrefix, prefixName, prefixNote, suffixName, suffixNote, 
                            replace, smartReplace, custom, customFormat, includeSubdirectories, useSequential, 1, conversion);
                        renamedCount += subdirRenamed;
                    } else {
                        // Rename the DIRECTORY itself
                        String newName = generateAdvancedFileName(file.getName(), conversionName, conversionNote,
                            separator, datePrefix, prefixName, prefixNote, suffixName, suffixNote,
                            replace, smartReplace, custom, customFormat, useSequential, 1);
                        
                        if (!newName.equals(file.getName())) {
                            File newFile = renameFileWithConflictResolution(file, newName, useSequential);
                            if (newFile != file) {
                                renamedCount++;
                                // Update linked file references to point to new directory path
                                updateLinkedFileReferences(conversion, file, newFile);
                            } else {
                                errorCount++;
                                System.err.println("Failed to rename directory: " + file.getAbsolutePath());
                            }
                        }
                    }
                } else {
                    // Skip common system-level files proactively
                    if (isSystemLevelFile(file.getName())) {
                        continue;
                    }
                    
                    // Handle individual file renaming
                    String newName = generateAdvancedFileName(file.getName(), conversionName, conversionNote,
                        separator, datePrefix, prefixName, prefixNote, suffixName, suffixNote,
                        replace, smartReplace, custom, customFormat, useSequential, 1);
                    
                    if (!newName.equals(file.getName())) {
                        File newFile = renameFileWithConflictResolution(file, newName, useSequential);
                        if (newFile != file) {
                            // Update linked file references to point to new file path
                            updateLinkedFileReferences(conversion, file, newFile);
                            renamedCount++;
                        } else {
                            errorCount++;
                            System.err.println("Failed to rename file: " + file.getAbsolutePath());
                        }
                    }
                }
            } catch (Exception e) {
                errorCount++;
                System.err.println("Error renaming file " + file.getAbsolutePath() + ": " + e.getMessage());
            }
        }

        // Show completion message with error count if any
        String message = "Advanced rename completed! Renamed " + renamedCount + " files.";
        if (errorCount > 0) {
            message += "\n" + errorCount + " files could not be renamed (check console for details).";
        }
        
        JOptionPane.showMessageDialog(null, message, "Rename Complete", 
            errorCount > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
        
        return renamedCount;
    }

    public static boolean isSystemLevelFile(String name) {
        String lower = name.toLowerCase();
        return lower.equals(".ds_store") || lower.equals("thumbs.db") ||
               lower.equals("desktop.ini") || lower.equals(".spotlight-v100") ||
               lower.equals(".trashes") || lower.equals(".temporaryitems") ||
               lower.startsWith("._");
    }

    /**
     * Rename a file with automatic conflict resolution and proper error handling.
     * 
     * @param file The file to rename
     * @param desiredName The desired new name
     * @param useSequential Whether to use zero-padded sequential numbering
     * @return The new file if rename succeeded, original file if it failed
     * @throws IllegalArgumentException if file is null or desiredName is empty
     */
    private static File renameFileWithConflictResolution(File file, String desiredName, boolean useSequential) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (desiredName == null || desiredName.trim().isEmpty()) {
            throw new IllegalArgumentException("Desired name cannot be null or empty");
        }
        
        // Validate that the file exists and is writable
        if (!file.exists()) {
            System.err.println("Warning: Cannot rename non-existent file: " + file.getAbsolutePath());
            return file;
        }
        if (!file.canWrite()) {
            System.err.println("Warning: Cannot rename read-only file: " + file.getAbsolutePath());
            return file;
        }
        
        File parentDir = file.getParentFile();
        if (parentDir == null || !parentDir.exists() || !parentDir.canWrite()) {
            System.err.println("Warning: Cannot rename file - parent directory issues: " + file.getAbsolutePath());
            return file;
        }
        
        String candidate = desiredName;
        
        // Extract base name and extension
        String base = candidate;
        String ext = "";
        int dotIdx = candidate.lastIndexOf('.');
        if (dotIdx > 0) {
            base = candidate.substring(0, dotIdx);
            ext = candidate.substring(dotIdx);
        }
        
        // Try the desired name first
        File targetFile = new File(parentDir, candidate);
        if (!targetFile.exists() || targetFile.equals(file)) {
            // No conflict, try to rename
            boolean success = file.renameTo(targetFile);
            if (success) {
                return targetFile;
            } else {
                System.err.println("Error: Failed to rename file from " + file.getAbsolutePath() + " to " + targetFile.getAbsolutePath());
                return file;
            }
        }
        
        // Conflict exists, find a unique name
        int counter = 1;
        int maxAttempts = 1000; // Prevent infinite loops
        do {
            String suffix = useSequential ? String.format(" (%03d)", counter) : " (" + counter + ")";
            candidate = base + suffix + ext;
            targetFile = new File(parentDir, candidate);
            counter++;
        } while (targetFile.exists() && counter <= maxAttempts);
        
        if (counter > maxAttempts) {
            System.err.println("Error: Could not find unique name for file after " + maxAttempts + " attempts: " + file.getAbsolutePath());
            return file;
        }
        
        // Try to rename with the unique name
        boolean success = file.renameTo(targetFile);
        if (success) {
            return targetFile;
        } else {
            System.err.println("Error: Failed to rename file from " + file.getAbsolutePath() + " to " + targetFile.getAbsolutePath());
            return file;
        }
    }

    private static int renameFilesInDirectoryAdvanced(File directory, String conversionName, String conversionNote,
            String separator, String datePrefix, boolean prefixName, boolean prefixNote,
            boolean suffixName, boolean suffixNote, boolean replace, boolean smartReplace,
            boolean custom, String customFormat, boolean includeSubdirectories, boolean useSequential, int startSequence, Conversion conversion) {
        
        File[] files = directory.listFiles();
        if (files == null) return 0;

        int count = 0;
        int sequenceNumber = startSequence;
        
        // Track used names to prevent conflicts
        java.util.Set<String> usedNames = new java.util.HashSet<>();

        for (File file : files) {
            if (file.isDirectory()) {
                // Only recurse into subdirectories if the includeSubdirectories option is enabled
                if (includeSubdirectories) {
                    count += renameFilesInDirectoryAdvanced(file, conversionName, conversionNote,
                        separator, datePrefix, prefixName, prefixNote, suffixName, suffixNote,
                        replace, smartReplace, custom, customFormat, includeSubdirectories, useSequential, sequenceNumber, conversion);
                }
                // Note: We don't rename the directory itself, only files
            } else {
                // Rename individual files
                String newName = generateAdvancedFileName(file.getName(), conversionName, conversionNote,
                    separator, datePrefix, prefixName, prefixNote, suffixName, suffixNote,
                    replace, smartReplace, custom, customFormat, useSequential, sequenceNumber);
                
                // Check for name conflicts and resolve them
                String finalName = newName;
                int conflictCounter = 1;
                while (usedNames.contains(finalName)) {
                    // Extract base name and extension, removing any existing numbering
                    String baseName = finalName;
                    String extension = "";
                    int dotIndex = finalName.lastIndexOf('.');
                    if (dotIndex > 0) {
                        baseName = finalName.substring(0, dotIndex);
                        extension = finalName.substring(dotIndex);
                    }
                    
                    // Remove any existing numbering pattern like " (1)", " (2)", etc.
                    baseName = baseName.replaceAll(" \\(\\d+\\)$", "");
                    
                    // Add sequential number to resolve conflict
                    finalName = baseName + " (" + conflictCounter + ")" + extension;
                    conflictCounter++;
                }
                
                // Add to used names set
                usedNames.add(finalName);
                
                if (!finalName.equals(file.getName())) {
                    File newFile = renameFileWithConflictResolution(file, finalName, useSequential);
                    if (newFile != file) {
                        // Update linked file references to point to new file path
                        updateLinkedFileReferences(conversion, file, newFile);
                        count++;
                    }
                }
                
                if (useSequential) {
                    sequenceNumber++;
                }
            }
        }
        return count;
    }

    private static String generateAdvancedFileName(String originalName, String conversionName, String conversionNote,
            String separator, String datePrefix, boolean prefixName, boolean prefixNote,
            boolean suffixName, boolean suffixNote, boolean replace, boolean smartReplace,
            boolean custom, String customFormat, boolean useSequential, int sequenceNumber) {
        
        String baseName = originalName;
        String extension = "";
        
        // Handle cases where there might be multiple dots (like "file.name.JPG")
        // Find the last dot that's followed by a valid file extension
        int dotIndex = -1;
        String lowerName = originalName.toLowerCase();
        
        // Common image/video extensions
        String[] commonExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".tiff", 
                                   ".mov", ".mp4", ".avi", ".wmv", ".flv", ".mkv",
                                   ".mp3", ".wav", ".aac", ".flac", ".wma"};
        
        // First, try to find the last valid extension
        for (String ext : commonExtensions) {
            int index = lowerName.lastIndexOf(ext);
            if (index > dotIndex) {
                dotIndex = index;
            }
        }
        
        // If no common extension found, fall back to last dot
        if (dotIndex == -1) {
            dotIndex = originalName.lastIndexOf('.');
        }
        
        // Additional safety: if we found a dot but it's not followed by a valid extension,
        // and there's another dot further back, try to find a better extension
        if (dotIndex > 0) {
            String potentialExtension = originalName.substring(dotIndex).toLowerCase();
            boolean hasValidExtension = false;
            for (String ext : commonExtensions) {
                if (potentialExtension.equals(ext)) {
                    hasValidExtension = true;
                    break;
                }
            }
            
            // If this extension isn't valid, try to find a better one
            if (!hasValidExtension) {
                int betterDotIndex = originalName.lastIndexOf('.', dotIndex - 1);
                if (betterDotIndex > 0) {
                    String betterExtension = originalName.substring(betterDotIndex).toLowerCase();
                    for (String ext : commonExtensions) {
                        if (betterExtension.equals(ext)) {
                            dotIndex = betterDotIndex;
                            break;
                        }
                    }
                }
            }
        }
        
        // Final safety check: if we still have a double extension issue,
        // try to find the most likely real extension by looking for patterns
        if (dotIndex > 0) {
            String currentExtension = originalName.substring(dotIndex).toLowerCase();
            // Check if this looks like a double extension (e.g., ".mp4.mp4")
            if (currentExtension.contains(".")) {
                // We have a double extension, try to find the real one
                String[] parts = currentExtension.split("\\.");
                if (parts.length > 1) {
                    // Look for the last valid extension part
                    for (int i = parts.length - 1; i >= 0; i--) {
                        String potentialExt = "." + parts[i];
                        for (String validExt : commonExtensions) {
                            if (potentialExt.equals(validExt)) {
                                // Found a valid extension, adjust dotIndex
                                dotIndex = originalName.length() - potentialExt.length();
                                break;
                            }
                        }
                        if (dotIndex != originalName.length() - currentExtension.length()) {
                            break; // Found a valid extension
                        }
                    }
                }
            }
        }
        
        if (dotIndex > 0) {
            baseName = originalName.substring(0, dotIndex);
            extension = originalName.substring(dotIndex);
        }

        String conversionInfo = conversionName;
        if ((prefixNote || suffixNote) && conversionNote != null && !conversionNote.trim().isEmpty()) {
            conversionInfo = conversionNote;
        }

        String result = datePrefix;

        if (custom && customFormat != null && !customFormat.trim().isEmpty()) {
            result += customFormat
                .replace("{conversion_name}", conversionName != null ? conversionName : "")
                .replace("{conversion_note}", conversionNote != null ? conversionNote : "")
                .replace("{original_name}", baseName) // baseName is already without extension
                .replace("{original_number}", useSequential ? String.format("%03d", sequenceNumber) : extractNumbersFromFilename(baseName));
        } else if (prefixName || prefixNote) {
            result += conversionInfo + separator + baseName;
        } else if (suffixName || suffixNote) {
            result += baseName + separator + conversionInfo;
        } else if (replace) {
            result += conversionInfo + (useSequential ? String.format(" (%03d)", sequenceNumber) : "");
        } else if (smartReplace) {
            if (isGenericFilename(baseName)) {
                result += conversionInfo + separator + (useSequential ? String.format("%03d", sequenceNumber) : extractNumbersFromFilename(baseName));
            } else {
                return originalName; // Keep original if not generic
            }
        } else {
            System.out.println("DEBUG: No rename strategy selected for file: " + originalName);
            return originalName; // No strategy selected, keep original
        }

        return result + extension;
    }

    private static boolean isGenericFilename(String filename) {
        String lower = filename.toLowerCase();
        return lower.startsWith("img_") || lower.startsWith("dsc_") || lower.startsWith("mov_") ||
               lower.startsWith("vid_") || lower.startsWith("pic_") || lower.startsWith("p") ||
               lower.startsWith("track") || lower.startsWith("audio_") || 
               lower.matches("\\d+") || lower.matches("track\\d+") || lower.matches("audio_\\d+") ||
               lower.startsWith("dsc") || lower.startsWith("img") || lower.startsWith("mov") ||
               lower.startsWith("vid") || lower.startsWith("pic") ||
               lower.endsWith("_img") || lower.endsWith("_dsc") || lower.endsWith("_mov") ||
               lower.endsWith("_vid") || lower.endsWith("_pic") || lower.endsWith("_photo") ||
               lower.endsWith("_image") || lower.endsWith("_audio") || lower.endsWith("_track") ||
               lower.matches(".*\\d{3,4}.*") || // Contains 3-4 digit numbers
               lower.matches(".*\\d{2,3}\\..*"); // Contains 2-3 digits before extension
    }

    private static String extractNumbersFromFilename(String filename) {
        String numbers = filename.replaceAll("[^0-9]", "");
        return numbers.isEmpty() ? "001" : numbers;
    }

    /**
     * Updates linked file references after a rename operation.
     * This method handles both individual files and directories.
     * 
     * @param conversion The conversion containing the linked files
     * @param oldFile The original file/directory path
     * @param newFile The new file/directory path after rename
     * @return true if any references were updated, false otherwise
     */
    private static boolean updateLinkedFileReferences(Conversion conversion, File oldFile, File newFile) {
        if (conversion.linkedFiles == null || conversion.linkedFiles.isEmpty()) {
            return false;
        }
        
        boolean updated = false;
        String oldPath = oldFile.getAbsolutePath();
        
        // Update any linked file references that point to the old path
        for (int i = 0; i < conversion.linkedFiles.size(); i++) {
            FileReference fileRef = conversion.linkedFiles.get(i);
            
            // Check if this reference points to the renamed file/directory
            if (fileRef.getPath().equals(oldPath)) {
                // Update to point to the new file/directory
                conversion.linkedFiles.set(i, new FileReference(newFile));
                updated = true;
            }
        }
        
        return updated;
    }
    
    /**
     * Updates linked file references after a batch rename operation.
     * This method scans the directory for renamed files and updates references accordingly.
     * 
     * @param conversion The conversion containing the linked files
     * @param baseName The base name used for renaming
     * @param parentDirectory The directory where files were renamed
     * @return true if any references were updated, false otherwise
     */
    public static boolean updateLinkedFileReferencesAfterBatchRename(Conversion conversion, String baseName, File parentDirectory) {
        if (conversion.linkedFiles == null || conversion.linkedFiles.isEmpty() || parentDirectory == null || !parentDirectory.exists()) {
            return false;
        }
        
        boolean updated = false;
        ArrayList<FileReference> updatedReferences = new ArrayList<>();
        
        // Get a snapshot of files in the directory to avoid race conditions
        File[] directoryFiles = parentDirectory.listFiles();
        if (directoryFiles == null) {
            return false;
        }
        
        // Create a map of normalized filenames to actual files for efficient lookup
        java.util.Map<String, File> fileMap = new java.util.HashMap<>();
        for (File file : directoryFiles) {
            if (file.isFile()) {
                fileMap.put(normalizeFilename(file.getName()), file);
            }
        }
        
        // Update references based on the renamed files
        for (FileReference oldRef : conversion.linkedFiles) {
            File oldFile = oldRef.getFile();
            
            // Check if the old file is in the same directory
            if (oldFile.getParentFile() != null && oldFile.getParentFile().equals(parentDirectory)) {
                String oldExtension = getFileExtension(oldFile.getName());
                String baseNorm = normalizeFilename(baseName);
                
                // Look for a file with the new base name and same extension
                File newFile = null;
                for (java.util.Map.Entry<String, File> entry : fileMap.entrySet()) {
                    String normalizedName = entry.getKey();
                    File candidateFile = entry.getValue();
                    
                    if (normalizedName.contains(baseNorm) && candidateFile.getName().endsWith(oldExtension)) {
                        newFile = candidateFile;
                        break;
                    }
                }
                
                if (newFile != null) {
                    updatedReferences.add(new FileReference(newFile));
                    updated = true;
                } else {
                    // Keep the old reference if no match found
                    updatedReferences.add(oldRef);
                }
            } else {
                // Keep references to files in other directories unchanged
                updatedReferences.add(oldRef);
            }
        }
        
        if (updated) {
            conversion.linkedFiles.clear();
            conversion.linkedFiles.addAll(updatedReferences);
        }
        
        return updated;
    }
    
    /**
     * Normalizes a filename for comparison by removing common variations.
     */
    private static String normalizeFilename(String filename) {
        if (filename == null) return "";
        return filename.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
    
    /**
     * Extracts the file extension from a filename.
     */
    private static String getFileExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex > 0) ? filename.substring(dotIndex) : "";
    }

    public static void relinkToTrimmedFiles(Project project) {
        // Prompt the user to select the directory containing trimmed files
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        // Set the current directory to the last used directory
        String lastDir = Preferences.getInstance().getLastUsedDirectory();
        if (lastDir != null && new File(lastDir).exists()) {
            fileChooser.setCurrentDirectory(new File(lastDir));
        }
        
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File trimmedDirectory = fileChooser.getSelectedFile();
            
            // Save the selected directory as the last used directory
            Preferences.getInstance().setLastUsedDirectory(trimmedDirectory.getAbsolutePath());

            // Create options dialog
            String[] options = {
                "Replace matching files with trimmed versions",
                "Add trimmed files to existing linked files"
            };

            int choice = JOptionPane.showOptionDialog(null,
                "How would you like to handle the trimmed files?",
                "Relink Options",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

            if (choice == -1) return; // User cancelled

            int replacedCount = 0;
            int addedCount = 0;
            int removedCount = 0;
            ArrayList<FileReference> filesToRemove = new ArrayList<>();

            // For each conversion in the project
            for (Conversion conversion : project.getConversions()) {
                ArrayList<FileReference> updatedFiles = new ArrayList<>();

                // For each linked file in the conversion
                for (FileReference oldFile : conversion.linkedFiles) {
                    String oldName = oldFile.getName();
                    String baseName = oldName.substring(0, oldName.lastIndexOf('.'));
                    String extension = oldName.substring(oldName.lastIndexOf('.'));
                    
                    // Simply append _trimmed to the original filename before the extension
                    String trimmedName = baseName + "_trimmed" + extension;
                    
                    File trimmedFile = new File(trimmedDirectory, trimmedName);

                    if (trimmedFile.exists()) {
                        if (choice == 0) {
                            // Option 1: Replace matching files
                            updatedFiles.add(new FileReference(trimmedFile));
                            replacedCount++;
                        } else {
                            // Option 2: Add trimmed files to existing
                            updatedFiles.add(oldFile);
                            updatedFiles.add(new FileReference(trimmedFile));
                            addedCount++;
                        }
                    } else {
                        // If trimmed file not found, ask user if they want to remove it
                        int removeChoice = JOptionPane.showConfirmDialog(null,
                            "No trimmed version found for: " + oldName + "\n\nWould you like to remove this file from the linked files?",
                            "File Not Found",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                        
                        if (removeChoice == JOptionPane.YES_OPTION) {
                            filesToRemove.add(oldFile);
                            removedCount++;
                        } else {
                            // Keep the original file if user chooses not to remove it
                            updatedFiles.add(oldFile);
                        }
                    }
                }

                // Update the conversion's linked files
                conversion.linkedFiles = updatedFiles;
            }

            // Notify the user of completion
            StringBuilder message = new StringBuilder();
            
            if (choice == 0) {
                message.append(String.format("%d files replaced with trimmed versions.", replacedCount));
            } else {
                message.append(String.format("%d trimmed files added to existing linked files.", addedCount));
            }
            
            if (removedCount > 0) {
                message.append(String.format("\n%d files were removed because no trimmed versions were found.", removedCount));
            }
            
            JOptionPane.showMessageDialog(null, message.toString(), "Relink Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}