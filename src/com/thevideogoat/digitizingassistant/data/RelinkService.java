package com.thevideogoat.digitizingassistant.data;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Unified service for handling all relink operations in the application.
 * Provides consistent behavior and settings across different relink contexts.
 */
public class RelinkService {
    
    public enum RelinkMode {
        INTERACTIVE,    // Show dialog with options
        QUICK,         // Use saved settings without dialog
        BULK_SMART     // Smart bulk relink with minimal user interaction
    }
    
    public enum RelinkScope {
        SINGLE_CONVERSION,  // Relink files for one conversion
        ALL_CONVERSIONS,    // Relink files for all conversions in project
        SELECTED_FILES      // Relink specific files
    }
    
    /**
     * Main relink method that handles all scenarios
     */
    public static RelinkResult performRelink(Project project, Conversion targetConversion, RelinkMode mode, RelinkScope scope, Component parent) {
        Preferences prefs = Preferences.getInstance();
        
        // Get directory - either from user or use saved
        File searchDirectory = null;
        if (mode == RelinkMode.INTERACTIVE || (mode != RelinkMode.INTERACTIVE && prefs.getLastUsedDirectory() == null)) {
            searchDirectory = selectDirectory(parent);
            if (searchDirectory == null) {
                return new RelinkResult(false, "No directory selected", 0);
            }
            prefs.setLastUsedDirectory(searchDirectory.getAbsolutePath());
        } else {
            String lastDir = prefs.getLastUsedDirectory();
            if (lastDir != null && new File(lastDir).exists()) {
                searchDirectory = new File(lastDir);
            } else {
                return new RelinkResult(false, "No previous relink directory found", 0);
            }
        }
        
        // Get search criteria
        RelinkCriteria criteria;
        if (mode == RelinkMode.INTERACTIVE) {
            criteria = showCriteriaDialog(parent, prefs);
            if (criteria == null) {
                return new RelinkResult(false, "User cancelled", 0);
            }
            // Save the criteria for future quick relinks
            saveCriteria(criteria, prefs);
        } else {
            criteria = loadSavedCriteria(prefs);
        }
        
        // Perform the relink based on scope
        switch (scope) {
            case SINGLE_CONVERSION:
                return relinkSingleConversion(targetConversion, searchDirectory, criteria, mode, parent);
            case ALL_CONVERSIONS:
                return relinkAllConversions(project, searchDirectory, criteria, mode, parent);
            case SELECTED_FILES:
                // This would be implemented for specific file selection scenarios
                return new RelinkResult(false, "Selected files relink not yet implemented", 0);
            default:
                return new RelinkResult(false, "Unknown relink scope", 0);
        }
    }
    
    /**
     * Specialized relink for trimmed files with specific naming pattern
     */
    public static RelinkResult performTrimmedRelink(Project project, Component parent) {
        Preferences prefs = Preferences.getInstance();
        
        // Get directory
        File searchDirectory = selectDirectory(parent);
        if (searchDirectory == null) {
            return new RelinkResult(false, "No directory selected", 0);
        }
        prefs.setLastUsedDirectory(searchDirectory.getAbsolutePath());
        
        // Show options dialog
        String[] options = {
            "Replace matching files with trimmed versions",
            "Add trimmed files to existing linked files"
        };

        int choice = JOptionPane.showOptionDialog(parent,
            "How would you like to handle the trimmed files?",
            "Relink Options",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);

        if (choice == -1) {
            return new RelinkResult(false, "User cancelled", 0);
        }
        
        int replacedCount = 0;
        int addedCount = 0;
        int removedCount = 0;
        
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
                
                File trimmedFile = new File(searchDirectory, trimmedName);

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
                    int removeChoice = JOptionPane.showConfirmDialog(parent,
                        "No trimmed version found for: " + oldName + "\n\nWould you like to remove this file from the linked files?",
                        "File Not Found",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                    
                    if (removeChoice == JOptionPane.YES_OPTION) {
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
        
        // Build result message
        StringBuilder message = new StringBuilder();
        
        if (choice == 0) {
            message.append(String.format("%d files replaced with trimmed versions.", replacedCount));
        } else {
            message.append(String.format("%d trimmed files added to existing linked files.", addedCount));
        }
        
        if (removedCount > 0) {
            message.append(String.format("\n%d files were removed because no trimmed versions were found.", removedCount));
        }
        
        return new RelinkResult(true, message.toString(), replacedCount + addedCount);
    }
    
    /**
     * Relink conversions by matching their notes to filenames in project directories
     */
    public static RelinkResult performRelinkByNote(Project project, java.util.List<FileReference> availableFiles) {
        int relinked = 0;
        
        for (Conversion c : project.getConversions()) {
            String noteNorm = normalizeFilename(c.note);
            if (noteNorm.isEmpty()) continue;
            
            for (FileReference fileRef : availableFiles) {
                String fileNorm = normalizeFilename(fileRef.getName());
                if (fileNorm.contains(noteNorm)) {
                    c.linkedFiles.clear();
                    c.linkedFiles.add(fileRef);
                    relinked++;
                    break;
                }
            }
        }
        
        String message = String.format("Relinked %d conversions by note.", relinked);
        return new RelinkResult(true, message, relinked);
    }
    
    /**
     * Helper method to normalize filenames for comparison (matches ProjectFrame logic)
     */
    private static String normalizeFilename(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        // Remove extension
        int dot = name.lastIndexOf('.');
        if (dot != -1) name = name.substring(0, dot);
        // Remove spaces, parentheses, dashes, underscores, and non-alphanumeric
        return name.replaceAll("[\\s\\(\\)\\-_.]", "").replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }
    
    /**
     * Smart bulk relink that tries to intelligently match files
     */
    public static RelinkResult performSmartBulkRelink(Project project, Component parent) {
        Preferences prefs = Preferences.getInstance();
        
        // Get directory
        File searchDirectory = selectDirectory(parent);
        if (searchDirectory == null) {
            return new RelinkResult(false, "No directory selected", 0);
        }
        prefs.setLastUsedDirectory(searchDirectory.getAbsolutePath());
        
        // Use all matching criteria for best results
        RelinkCriteria criteria = new RelinkCriteria(true, true, true);
        
        // Show progress dialog for bulk operation
        return relinkAllConversionsWithProgress(project, searchDirectory, criteria, parent);
    }
    
    private static File selectDirectory(Component parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select directory to search for media files");
        
        String lastDir = Preferences.getInstance().getLastUsedDirectory();
        if (lastDir != null && new File(lastDir).exists()) {
            fileChooser.setCurrentDirectory(new File(lastDir));
        }
        
        int result = fileChooser.showOpenDialog(parent);
        return (result == JFileChooser.APPROVE_OPTION) ? fileChooser.getSelectedFile() : null;
    }
    
    private static RelinkCriteria showCriteriaDialog(Component parent, Preferences prefs) {
        JCheckBox byNote = new JCheckBox("Match conversion note", prefs.isRelinkByNote());
        JCheckBox byTitle = new JCheckBox("Match conversion title", prefs.isRelinkByTitle());
        JCheckBox byTrimmed = new JCheckBox("Match trimmed filenames (_trimmed)", prefs.isRelinkByTrimmed());
        
        JPanel opts = new JPanel(new GridLayout(0, 1));
        opts.add(byNote);
        opts.add(byTitle);
        opts.add(byTrimmed);
        
        int result = JOptionPane.showConfirmDialog(parent, opts, "Relink Search Options", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        
        return new RelinkCriteria(byNote.isSelected(), byTitle.isSelected(), byTrimmed.isSelected());
    }
    
    private static RelinkCriteria loadSavedCriteria(Preferences prefs) {
        return new RelinkCriteria(prefs.isRelinkByNote(), prefs.isRelinkByTitle(), prefs.isRelinkByTrimmed());
    }
    
    private static void saveCriteria(RelinkCriteria criteria, Preferences prefs) {
        prefs.setRelinkByNote(criteria.byNote);
        prefs.setRelinkByTitle(criteria.byTitle);
        prefs.setRelinkByTrimmed(criteria.byTrimmed);
    }
    
    private static RelinkResult relinkSingleConversion(Conversion conversion, File searchDirectory, RelinkCriteria criteria, RelinkMode mode, Component parent) {
        List<File> matches = findMatchingFiles(conversion, searchDirectory, criteria);
        
        if (matches.isEmpty()) {
            String message = "No matching files found" + (mode == RelinkMode.QUICK ? " using saved settings" : "");
            return new RelinkResult(false, message, 0);
        }
        
        File selectedFile;
        if (mode == RelinkMode.QUICK || mode == RelinkMode.BULK_SMART) {
            // Auto-select the best match
            selectedFile = matches.get(0);
        } else {
            // Let user choose from matches
            selectedFile = showFileSelectionDialog(matches, parent);
            if (selectedFile == null) {
                return new RelinkResult(false, "No file selected", 0);
            }
        }
        
        // Update the conversion's linked files
        if (conversion.linkedFiles == null) conversion.linkedFiles = new ArrayList<>();
        conversion.linkedFiles.clear();
        conversion.linkedFiles.add(new FileReference(selectedFile.getAbsolutePath()));
        
        String message = mode == RelinkMode.QUICK ? 
            "Quick relink completed. Linked to: " + selectedFile.getName() :
            "Relink completed. Linked to: " + selectedFile.getName();
        
        return new RelinkResult(true, message, 1);
    }
    
    private static RelinkResult relinkAllConversions(Project project, File searchDirectory, RelinkCriteria criteria, RelinkMode mode, Component parent) {
        int successCount = 0;
        int totalConversions = project.getConversions().size();
        
        for (Conversion conversion : project.getConversions()) {
            List<File> matches = findMatchingFiles(conversion, searchDirectory, criteria);
            if (!matches.isEmpty()) {
                // Auto-select best match for bulk operations
                File bestMatch = matches.get(0);
                if (conversion.linkedFiles == null) conversion.linkedFiles = new ArrayList<>();
                conversion.linkedFiles.clear();
                conversion.linkedFiles.add(new FileReference(bestMatch.getAbsolutePath()));
                successCount++;
            }
        }
        
        String message = String.format("Bulk relink completed. Successfully relinked %d of %d conversions.", successCount, totalConversions);
        return new RelinkResult(true, message, successCount);
    }
    
    private static RelinkResult relinkAllConversionsWithProgress(Project project, File searchDirectory, RelinkCriteria criteria, Component parent) {
        // Create progress dialog
        JProgressBar progressBar = new JProgressBar(0, project.getConversions().size());
        progressBar.setStringPainted(true);
        
        JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Smart Bulk Relink", true);
        progressDialog.setLayout(new BorderLayout());
        progressDialog.add(new JLabel("Processing conversions..."), BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);
        progressDialog.setSize(400, 100);
        progressDialog.setLocationRelativeTo(parent);
        
        // Use SwingWorker for background processing
        SwingWorker<RelinkResult, Integer> worker = new SwingWorker<RelinkResult, Integer>() {
            @Override
            protected RelinkResult doInBackground() throws Exception {
                int successCount = 0;
                int processed = 0;
                
                for (Conversion conversion : project.getConversions()) {
                    List<File> matches = findMatchingFiles(conversion, searchDirectory, criteria);
                    if (!matches.isEmpty()) {
                        File bestMatch = matches.get(0);
                        if (conversion.linkedFiles == null) conversion.linkedFiles = new ArrayList<>();
                        conversion.linkedFiles.clear();
                        conversion.linkedFiles.add(new FileReference(bestMatch.getAbsolutePath()));
                        successCount++;
                    }
                    processed++;
                    publish(processed);
                    
                    // Small delay to show progress
                    Thread.sleep(50);
                }
                
                return new RelinkResult(true, 
                    String.format("Smart bulk relink completed. Successfully relinked %d of %d conversions.", 
                    successCount, project.getConversions().size()), successCount);
            }
            
            @Override
            protected void process(List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    progressBar.setValue(chunks.get(chunks.size() - 1));
                    progressBar.setString(String.format("Processing %d of %d...", 
                        chunks.get(chunks.size() - 1), project.getConversions().size()));
                }
            }
            
            @Override
            protected void done() {
                progressDialog.dispose();
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true);
        
        try {
            return worker.get();
        } catch (Exception e) {
            return new RelinkResult(false, "Error during bulk relink: " + e.getMessage(), 0);
        }
    }
    
    private static List<File> findMatchingFiles(Conversion conversion, File searchDirectory, RelinkCriteria criteria) {
        List<File> matches = new ArrayList<>();
        
        Predicate<File> predicate = f -> {
            if (f.isDirectory()) return false;
            String lowerName = f.getName().toLowerCase();
            if (lowerName.endsWith(".llc")) return false; // ignore LosslessCut project files

            boolean matched = false;

            // Exact match on note (base name equals conversion note), any extension
            if (criteria.byNote && conversion.note != null && !conversion.note.isBlank()) {
                String base = lowerName;
                int dot = base.lastIndexOf('.');
                if (dot > 0) base = base.substring(0, dot);
                matched |= base.equals(conversion.note.toLowerCase());
            }

            // Exact match on title with .mp4 extension
            if (!matched && criteria.byTitle) {
                String base = lowerName;
                int dot = base.lastIndexOf('.');
                String ext = "";
                if (dot > 0) {
                    ext = base.substring(dot);
                    base = base.substring(0, dot);
                }
                matched |= base.equals(conversion.name.toLowerCase()) && ext.equals(".mp4");
            }

            // Trimmed match: title_... with "trimmed" somewhere
            if (!matched && criteria.byTrimmed) {
                String baseTitle = conversion.name.toLowerCase();
                matched |= lowerName.startsWith(baseTitle + "_") && lowerName.contains("trimmed");
            }
            return matched;
        };

        Deque<File> stack = new ArrayDeque<>();
        stack.push(searchDirectory);
        while (!stack.isEmpty()) {
            File dir = stack.pop();
            File[] list = dir.listFiles();
            if (list == null) continue;
            for (File f : list) {
                if (f.isDirectory()) {
                    stack.push(f);
                    continue;
                }
                if (predicate.test(f)) matches.add(f);
            }
        }

        // Prioritize results: 1) note exact, 2) trimmed, 3) title exact mp4
        ToIntFunction<File> rank = f -> {
            String lowerName = f.getName().toLowerCase();
            String base = lowerName;
            int dot = base.lastIndexOf('.');
            String ext = "";
            if (dot > 0) {
                ext = base.substring(dot);
                base = base.substring(0, dot);
            }

            // note exact
            if (criteria.byNote && conversion.note != null && !conversion.note.isBlank()) {
                if (base.equals(conversion.note.toLowerCase())) return 0;
            }
            // trimmed
            if (criteria.byTrimmed) {
                String baseTitle = conversion.name.toLowerCase();
                if (lowerName.startsWith(baseTitle + "_") && lowerName.contains("trimmed")) return 1;
            }
            // title exact mp4
            if (criteria.byTitle) {
                if (base.equals(conversion.name.toLowerCase()) && ext.equals(".mp4")) return 2;
            }
            return 3;
        };
        matches.sort(Comparator.comparingInt(rank).thenComparing(File::getName));

        return matches;
    }
    
    private static File showFileSelectionDialog(List<File> matches, Component parent) {
        JList<File> list = new JList<>(matches.toArray(new File[0]));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0); // Pre-select the best match
        
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(600, 300));
        
        int result = JOptionPane.showConfirmDialog(parent, scrollPane, "Select file to link", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        
        return list.getSelectedValue();
    }
    
    /**
     * Helper class to hold relink criteria
     */
    public static class RelinkCriteria {
        public final boolean byNote;
        public final boolean byTitle;
        public final boolean byTrimmed;
        
        public RelinkCriteria(boolean byNote, boolean byTitle, boolean byTrimmed) {
            this.byNote = byNote;
            this.byTitle = byTitle;
            this.byTrimmed = byTrimmed;
        }
    }
    
    /**
     * Helper class to hold relink results
     */
    public static class RelinkResult {
        public final boolean success;
        public final String message;
        public final int filesRelinked;
        
        public RelinkResult(boolean success, String message, int filesRelinked) {
            this.success = success;
            this.message = message;
            this.filesRelinked = filesRelinked;
        }
    }
}
