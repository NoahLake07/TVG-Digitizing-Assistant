package com.thevideogoat.digitizingassistant.data;

import com.thevideogoat.digitizingassistant.ui.DigitizingAssistant;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ListIterator;

public class Util {

    public static File renameFile(File file, String newName){
        String extension = "";
        int i = file.getName().lastIndexOf('.');
        if (i > 0) {
            extension = file.getName().substring(i);
        }
        
        // Check if newName already contains the extension
        if (newName.toLowerCase().endsWith(extension.toLowerCase())) {
            // newName already has the extension, don't add it again
            File newFile = new File(file.getParentFile(), newName);
            boolean success = file.renameTo(newFile);
            return success ? newFile : file; // Return original file if rename failed
        } else {
            // Add the extension to newName
            File newFile = new File(file.getParentFile(), newName + extension);
            boolean success = file.renameTo(newFile);
            return success ? newFile : file; // Return original file if rename failed
        }
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

    public static void renameFilesWithOptions(ArrayList<File> files, String newName, 
        boolean includeSubdirectories, boolean preserveNumbering) {
        
        if (files.isEmpty()) {
            return;
        }

        int renamedCount = 0;
        for (File file : files) {
            if (file.isDirectory() && includeSubdirectories) {
                // Rename all files in directory and subdirectories
                renamedCount += renameFilesInDirectory(file, newName, includeSubdirectories, preserveNumbering);
            } else {
                // Rename just this file
                String finalName = preserveNumbering ? 
                    preserveNumberInFilename(file.getName(), newName) : 
                    newName;
                renameFile(file, finalName);
                renamedCount++;
            }
        }

        JOptionPane.showMessageDialog(null, 
            "Renamed " + renamedCount + " files/directories.", 
            "Rename Success", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    // Adapter method for FileReference support
    public static void renameFilesWithOptionsFromReferences(ArrayList<FileReference> fileRefs, String newName, 
        boolean includeSubdirectories, boolean preserveNumbering) {
        
        if (fileRefs.isEmpty()) {
            return;
        }

        // Convert FileReferences to Files
        ArrayList<File> files = new ArrayList<>();
        for (FileReference fileRef : fileRefs) {
            files.add(fileRef.getFile());
        }
        
        // Call the original method
        renameFilesWithOptions(files, newName, includeSubdirectories, preserveNumbering);
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

    public static void renameFilesWithAdvancedOptions(ArrayList<File> files, String conversionName, String conversionNote,
            String separator, boolean addDate, boolean prefixName, boolean prefixNote, 
            boolean suffixName, boolean suffixNote, boolean replace, boolean smartReplace, 
            boolean custom, String customFormat, boolean includeSubdirectories, boolean useSequential,
            Conversion conversion) {
        
        if (files.isEmpty()) {
            return;
        }

        int renamedCount = 0;
        int sequenceNumber = 1;
        String datePrefix = addDate ? java.time.LocalDate.now().toString() + separator : "";

        // Track used names per parent directory to avoid collisions across the batch
        java.util.Map<File, java.util.Set<String>> dirToUsedNames = new java.util.HashMap<>();

        for (File file : files) {
            if (file.isDirectory()) {
                if (includeSubdirectories) {
                    // Rename files INSIDE the directory (keep directory name)
                    renamedCount += renameFilesInDirectoryAdvanced(file, conversionName, conversionNote, 
                        separator, datePrefix, prefixName, prefixNote, suffixName, suffixNote, 
                        replace, smartReplace, custom, customFormat, includeSubdirectories, useSequential, sequenceNumber, conversion);
                } else {
                    // Rename the DIRECTORY itself
                    String newName = generateAdvancedFileName(file.getName(), conversionName, conversionNote,
                        separator, datePrefix, prefixName, prefixNote, suffixName, suffixNote,
                        replace, smartReplace, custom, customFormat, useSequential, sequenceNumber);
                    
                    if (!newName.equals(file.getName())) {
                        File newFile = renameFile(file, newName);
                        if (newFile != file) {
                            renamedCount++;
                            
                            // Update linked file references to point to new directory path
                            updateLinkedFileReferences(conversion, file, newFile);
                        }
                    }
                    
                    if (useSequential) {
                        sequenceNumber++;
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
                    replace, smartReplace, custom, customFormat, useSequential, sequenceNumber);
                
                // Ensure uniqueness in the target directory, even if useSequential is off
                File parentDir = file.getParentFile();
                java.util.Set<String> used = dirToUsedNames.computeIfAbsent(parentDir, d -> new java.util.HashSet<>());
                String candidate = newName;
                String base = candidate;
                String ext = "";
                int dotIdx = candidate.lastIndexOf('.');
                if (dotIdx > 0) {
                    base = candidate.substring(0, dotIdx);
                    ext = candidate.substring(dotIdx);
                }
                int conflictCounter = 1;
                while (used.contains(candidate) || new File(parentDir, candidate).exists()) {
                    // Strip existing numbering like " (1)" or " (001)"
                    String cleanBase = base.replaceAll(" \\((?:\\d+|\\d{3})\\)$", "");
                    String suffix = useSequential ? String.format(" (%03d)", conflictCounter) : " (" + conflictCounter + ")";
                    candidate = cleanBase + suffix + ext;
                    conflictCounter++;
                }

                if (!candidate.equals(file.getName())) {
                    File newFile = renameFile(file, candidate);
                    if (newFile != file) {
                        // Update linked file references to point to new file path
                        updateLinkedFileReferences(conversion, file, newFile);
                        renamedCount++;
                        used.add(candidate);
                    }
                }
                
                if (useSequential) {
                    sequenceNumber++;
                }
            }
        }

        JOptionPane.showMessageDialog(null, 
            "Advanced rename completed! Renamed " + renamedCount + " files.", 
            "Rename Success", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean isSystemLevelFile(String name) {
        String lower = name.toLowerCase();
        return lower.equals(".ds_store") || lower.equals("thumbs.db") ||
               lower.equals("desktop.ini") || lower.equals(".spotlight-v100") ||
               lower.equals(".trashes") || lower.equals(".temporaryitems") ||
               lower.startsWith("._");
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
                    File newFile = renameFile(file, finalName);
                    // Update linked file references to point to new file path
                    updateLinkedFileReferences(conversion, file, newFile);
                    count++;
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

    private static void updateLinkedFileReferences(Conversion conversion, File oldFile, File newFile) {
        if (conversion.linkedFiles == null) return;
        
        // Update any linked file references that point to the old directory
        for (int i = 0; i < conversion.linkedFiles.size(); i++) {
            FileReference fileRef = conversion.linkedFiles.get(i);
            
            // Check if this reference points to the renamed directory
            if (fileRef.getPath().equals(oldFile.getAbsolutePath())) {
                // Update to point to the new directory
                conversion.linkedFiles.set(i, new FileReference(newFile));
            }
        }
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