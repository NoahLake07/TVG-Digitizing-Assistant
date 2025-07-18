package com.thevideogoat.digitizingassistant.util;

import com.thevideogoat.digitizingassistant.data.FileReference;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages file operations and caching to improve performance.
 * Handles background file scanning, size calculations, and metadata retrieval.
 */
public class FileCacheManager {
    
    private static FileCacheManager instance;
    private final ExecutorService executorService;
    private final Map<String, FileMetadata> metadataCache;
    private final Map<String, Long> sizeCache;
    private final Set<String> scanningDirectories;
    private final AtomicLong totalCachedSize;
    
    private FileCacheManager() {
        this.executorService = Executors.newFixedThreadPool(2);
        this.metadataCache = new ConcurrentHashMap<>();
        this.sizeCache = new ConcurrentHashMap<>();
        this.scanningDirectories = ConcurrentHashMap.newKeySet();
        this.totalCachedSize = new AtomicLong(0);
    }
    
    public static synchronized FileCacheManager getInstance() {
        if (instance == null) {
            instance = new FileCacheManager();
        }
        return instance;
    }
    
    /**
     * Get file metadata (size, exists, etc.) with caching
     */
    public FileMetadata getFileMetadata(FileReference fileRef) {
        String path = fileRef.getPath();
        FileMetadata metadata = metadataCache.get(path);
        
        if (metadata == null || metadata.isStale()) {
            metadata = new FileMetadata(fileRef.getFile());
            metadataCache.put(path, metadata);
        }
        
        return metadata;
    }
    
    /**
     * Calculate total size of files asynchronously
     */
    public CompletableFuture<Long> calculateTotalSizeAsync(List<FileReference> files) {
        return CompletableFuture.supplyAsync(() -> {
            long totalSize = 0;
            for (FileReference fileRef : files) {
                FileMetadata metadata = getFileMetadata(fileRef);
                if (metadata.exists()) {
                    if (metadata.isDirectory()) {
                        totalSize += calculateDirectorySizeAsync(fileRef.getFile()).join();
                    } else {
                        totalSize += metadata.getSize();
                    }
                }
            }
            return totalSize;
        }, executorService);
    }
    
    /**
     * Calculate directory size recursively asynchronously
     */
    public CompletableFuture<Long> calculateDirectorySizeAsync(File directory) {
        return CompletableFuture.supplyAsync(() -> {
            String dirPath = directory.getAbsolutePath();
            
            // Check if already scanning this directory
            if (scanningDirectories.contains(dirPath)) {
                return 0L; // Return 0 to avoid infinite recursion
            }
            
            scanningDirectories.add(dirPath);
            try {
                return calculateDirectorySizeRecursive(directory);
            } finally {
                scanningDirectories.remove(dirPath);
            }
        }, executorService);
    }
    
    private long calculateDirectorySizeRecursive(File directory) {
        long size = 0;
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += calculateDirectorySizeRecursive(file);
                    }
                }
            }
        }
        return size;
    }
    
    /**
     * Preload metadata for a list of files in the background
     */
    public void preloadMetadataAsync(List<FileReference> files) {
        CompletableFuture.runAsync(() -> {
            for (FileReference fileRef : files) {
                getFileMetadata(fileRef);
            }
        }, executorService);
    }
    
    /**
     * Clear cache to free memory
     */
    public void clearCache() {
        metadataCache.clear();
        sizeCache.clear();
        totalCachedSize.set(0);
    }
    
    /**
     * Invalidate cache for specific files
     */
    public void invalidateCache(List<FileReference> files) {
        for (FileReference fileRef : files) {
            metadataCache.remove(fileRef.getPath());
            sizeCache.remove(fileRef.getPath());
        }
    }
    
    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * File metadata holder with caching
     */
    public static class FileMetadata {
        private final long size;
        private final boolean exists;
        private final boolean isDirectory;
        private final long lastModified;
        private final long cacheTime;
        
        public FileMetadata(File file) {
            this.exists = file.exists();
            this.isDirectory = exists && file.isDirectory();
            this.size = exists ? file.length() : 0;
            this.lastModified = exists ? file.lastModified() : 0;
            this.cacheTime = System.currentTimeMillis();
        }
        
        public long getSize() { return size; }
        public boolean exists() { return exists; }
        public boolean isDirectory() { return isDirectory; }
        public long getLastModified() { return lastModified; }
        
        /**
         * Check if cached data is stale (older than 30 seconds)
         */
        public boolean isStale() {
            return System.currentTimeMillis() - cacheTime > 30000;
        }
    }
} 