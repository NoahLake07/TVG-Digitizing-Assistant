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
        File newFile = new File(file.getParentFile(), newName + extension);
        file.renameTo(newFile);
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

        ListIterator<File> iterator = c.linkedFiles.listIterator();
        int i = 0;
        while(iterator.hasNext()){
            File f = iterator.next();
            ++i;
            // Rename the file and replace it in the list
            iterator.set(renameFile(f, c.name + (i > 1 ? " (" + i + ")" : "")));
        }

        // Completion dialog
        JOptionPane.showMessageDialog(null, "Renamed " + i + " files.", "Rename Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void renameLinkedFilesToNote(Conversion c){
        if(c.linkedFiles.isEmpty()){
            return;
        }

        ListIterator<File> iterator = c.linkedFiles.listIterator();
        int i = 0;
        while(iterator.hasNext()){
            File f = iterator.next();
            ++i;
            // Rename the file and replace it in the list
            iterator.set(renameFile(f, c.note + (i > 1 ? " (" + i + ")" : "")));
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
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File newDirectory = fileChooser.getSelectedFile();

            // For each conversion in the project
            int i = 0, f = 0;
            for (Conversion conversion : project.getConversions()) {
                ArrayList<File> updatedFiles = new ArrayList<>();

                // For each linked file in the conversion
                for (File oldFile : conversion.linkedFiles) {
                    File newFile = new File(newDirectory, oldFile.getName());

                    // If a file with the same name is found in the new directory
                    if (newFile.exists()) {
                        updatedFiles.add(newFile);
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

    public static ArrayList<File> getLinkedFiles(Project p){
        ArrayList<File> linkedFiles = new ArrayList<>();
        for (Conversion conversion : p.conversions) {
            linkedFiles.addAll(conversion.linkedFiles);
        }
        return linkedFiles;
    }

    public static ArrayList<File> getVideoFiles(Project p){
        ArrayList<File> videoFiles = new ArrayList<>();
        for (File file : getLinkedFiles(p)) {
            if (Util.isVideoFile(file)) {
                videoFiles.add(file);
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

    public static void relinkToTrimmedFiles(Project project) {
        // Prompt the user to select the directory containing trimmed files
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File trimmedDirectory = fileChooser.getSelectedFile();

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
            ArrayList<File> filesToRemove = new ArrayList<>();

            // For each conversion in the project
            for (Conversion conversion : project.getConversions()) {
                ArrayList<File> updatedFiles = new ArrayList<>();

                // For each linked file in the conversion
                for (File oldFile : conversion.linkedFiles) {
                    String oldName = oldFile.getName();
                    String baseName = oldName.substring(0, oldName.lastIndexOf('.'));
                    String extension = oldName.substring(oldName.lastIndexOf('.'));
                    
                    // Simply append _trimmed to the original filename before the extension
                    String trimmedName = baseName + "_trimmed" + extension;
                    
                    File trimmedFile = new File(trimmedDirectory, trimmedName);

                    if (trimmedFile.exists()) {
                        if (choice == 0) {
                            // Option 1: Replace matching files
                            updatedFiles.add(trimmedFile);
                            replacedCount++;
                        } else {
                            // Option 2: Add trimmed files to existing
                            updatedFiles.add(oldFile);
                            updatedFiles.add(trimmedFile);
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