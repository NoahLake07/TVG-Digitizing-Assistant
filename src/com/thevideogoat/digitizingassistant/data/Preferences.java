package com.thevideogoat.digitizingassistant.data;

import com.thevideogoat.digitizingassistant.ui.DigitizingAssistant;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class Preferences {
    private static final String PREFERENCES_FILE = "preferences.properties";
    private static Preferences instance;
    
    private String lastUsedDirectory;
    private Type lastUsedConversionType;
    
    private Preferences() {
        // Default values
        lastUsedDirectory = System.getProperty("user.home");
        lastUsedConversionType = Type.VHS;
        loadPreferences();
    }
    
    public static Preferences getInstance() {
        if (instance == null) {
            instance = new Preferences();
        }
        return instance;
    }
    
    public String getLastUsedDirectory() {
        return lastUsedDirectory;
    }
    
    public void setLastUsedDirectory(String directory) {
        this.lastUsedDirectory = directory;
        savePreferences();
    }
    
    public Type getLastUsedConversionType() {
        return lastUsedConversionType;
    }
    
    public void setLastUsedConversionType(Type type) {
        this.lastUsedConversionType = type;
        savePreferences();
    }
    
    private File getPreferencesFile() {
        return Paths.get(DigitizingAssistant.PROJECTS_DIRECTORY.getParent(), PREFERENCES_FILE).toFile();
    }
    
    private void loadPreferences() {
        File prefsFile = getPreferencesFile();
        if (prefsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(prefsFile)) {
                Properties props = new Properties();
                props.load(fis);
                
                String dir = props.getProperty("lastUsedDirectory");
                if (dir != null && new File(dir).exists()) {
                    lastUsedDirectory = dir;
                }
                
                String typeStr = props.getProperty("lastUsedConversionType");
                if (typeStr != null) {
                    try {
                        lastUsedConversionType = Type.fromDisplayName(typeStr);
                    } catch (Exception e) {
                        // Use default if parsing fails
                        lastUsedConversionType = Type.VHS;
                    }
                }
            } catch (IOException e) {
                // Use defaults if loading fails
                System.err.println("Could not load preferences: " + e.getMessage());
            }
        }
    }
    
    private void savePreferences() {
        try {
            File prefsFile = getPreferencesFile();
            prefsFile.getParentFile().mkdirs();
            
            Properties props = new Properties();
            props.setProperty("lastUsedDirectory", lastUsedDirectory);
            props.setProperty("lastUsedConversionType", lastUsedConversionType.toString());
            
            try (FileOutputStream fos = new FileOutputStream(prefsFile)) {
                props.store(fos, "Digitizing Assistant Preferences");
            }
        } catch (IOException e) {
            System.err.println("Could not save preferences: " + e.getMessage());
        }
    }
} 