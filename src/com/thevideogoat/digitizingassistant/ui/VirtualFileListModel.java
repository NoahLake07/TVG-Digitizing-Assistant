package com.thevideogoat.digitizingassistant.ui;

import com.thevideogoat.digitizingassistant.data.FileReference;
import com.thevideogoat.digitizingassistant.util.FileCacheManager;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Virtual list model for file references that loads data on demand.
 * This improves performance when dealing with large numbers of files.
 */
public class VirtualFileListModel extends AbstractListModel<FileReference> {
    
    private final List<FileReference> files;
    private final FileCacheManager cacheManager;
    private boolean isLoading = false;
    
    public VirtualFileListModel(List<FileReference> files) {
        this.files = new ArrayList<>(files);
        this.cacheManager = FileCacheManager.getInstance();
        
        // Preload metadata in background if there are many files
        if (files.size() > 50) {
            preloadMetadataAsync();
        }
    }
    
    @Override
    public int getSize() {
        return files.size();
    }
    
    @Override
    public FileReference getElementAt(int index) {
        if (index >= 0 && index < files.size()) {
            FileReference fileRef = files.get(index);
            
            // Trigger metadata loading in background if not already cached
            if (!isLoading) {
                cacheManager.getFileMetadata(fileRef);
            }
            
            return fileRef;
        }
        return null;
    }
    
    /**
     * Add a file reference to the list
     */
    public void addElement(FileReference fileRef) {
        files.add(fileRef);
        fireIntervalAdded(this, files.size() - 1, files.size() - 1);
    }
    
    /**
     * Remove a file reference from the list
     */
    public void removeElement(FileReference fileRef) {
        int index = files.indexOf(fileRef);
        if (index != -1) {
            files.remove(index);
            fireIntervalRemoved(this, index, index);
        }
    }
    
    /**
     * Update the entire list
     */
    public void updateList(List<FileReference> newFiles) {
        int oldSize = files.size();
        files.clear();
        files.addAll(newFiles);
        
        if (oldSize > 0) {
            fireIntervalRemoved(this, 0, oldSize - 1);
        }
        if (files.size() > 0) {
            fireIntervalAdded(this, 0, files.size() - 1);
        }
        
        // Preload metadata for new files
        if (files.size() > 50) {
            preloadMetadataAsync();
        }
    }
    
    /**
     * Get all file references
     */
    public List<FileReference> getAllFiles() {
        return new ArrayList<>(files);
    }
    
    /**
     * Preload metadata for all files in background
     */
    private void preloadMetadataAsync() {
        if (isLoading) return;
        
        isLoading = true;
        CompletableFuture.runAsync(() -> {
            try {
                cacheManager.preloadMetadataAsync(files);
            } finally {
                isLoading = false;
            }
        });
    }
    
    /**
     * Clear the list
     */
    public void clear() {
        int size = files.size();
        files.clear();
        if (size > 0) {
            fireIntervalRemoved(this, 0, size - 1);
        }
    }
    
    /**
     * Check if list is empty
     */
    public boolean isEmpty() {
        return files.isEmpty();
    }
    
    /**
     * Get the number of files
     */
    public int size() {
        return files.size();
    }
} 