package com.thevideogoat.digitizingassistant.data;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;

/**
 * Lightweight file reference that stores only the path and loads File objects on demand.
 * This reduces memory usage when projects have many linked files.
 */
public class FileReference implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    private final String filePath;
    private transient File cachedFile;
    private transient Long cachedSize;
    private transient Boolean cachedExists;
    
    public FileReference(String filePath) {
        this.filePath = filePath;
    }
    
    public FileReference(File file) {
        this.filePath = file.getAbsolutePath();
        this.cachedFile = file;
    }
    
    public String getPath() {
        return filePath;
    }
    
    /**
     * Get the File object, creating it on demand if not cached
     */
    public File getFile() {
        if (cachedFile == null) {
            cachedFile = new File(filePath);
        }
        return cachedFile;
    }
    
    /**
     * Check if file exists, with caching
     */
    public boolean exists() {
        if (cachedExists == null) {
            cachedExists = getFile().exists();
        }
        return cachedExists;
    }
    
    /**
     * Get file size, with caching
     */
    public long length() {
        if (cachedSize == null) {
            cachedSize = exists() ? getFile().length() : 0L;
        }
        return cachedSize;
    }
    
    /**
     * Get file name
     */
    public String getName() {
        return getFile().getName();
    }
    
    /**
     * Get parent directory
     */
    public File getParentFile() {
        return getFile().getParentFile();
    }
    
    /**
     * Clear cached data to free memory
     */
    public void clearCache() {
        cachedFile = null;
        cachedSize = null;
        cachedExists = null;
    }
    
    /**
     * Invalidate cache when file might have changed
     */
    public void invalidateCache() {
        cachedSize = null;
        cachedExists = null;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FileReference that = (FileReference) obj;
        return filePath.equals(that.filePath);
    }
    
    @Override
    public int hashCode() {
        return filePath.hashCode();
    }
    
    @Override
    public String toString() {
        return filePath;
    }
} 