package com.thevideogoat.digitizingassistant.data;

import com.thevideogoat.digitizingassistant.ui.DigitizingAssistant;

import java.io.*;
import java.nio.file.Paths;
import java.util.Properties;

public class Preferences {
    private static final String PREFERENCES_FILE = "preferences.properties";
    private static Preferences instance;
    
    private String lastUsedDirectory;
    private Type lastUsedConversionType;
    // Advanced rename persisted options
    private String renameSeparator = " - ";
    private boolean renameAddDate = false;
    private boolean renameIncludeSubdirs = false;
    private boolean renameUseSequential = false;
    private boolean renameIgnoreSystemFiles = true;
    private boolean renameDeleteIgnored = false;
    private boolean stratPrefixName = true;
    private boolean stratPrefixNote = false;
    private boolean stratSuffixName = false;
    private boolean stratSuffixNote = false;
    private boolean stratReplaceNote = false;
    private boolean stratNoteNumber = false;
    private boolean stratReplace = false;
    private boolean stratSmartReplace = false;
    private boolean stratCustom = false;
    private String stratCustomFormat = "{conversion_name} - {original_name}";
    
    // Relink settings
    private boolean relinkByNote = true;
    private boolean relinkByTitle = true;
    private boolean relinkByTrimmed = true;
    
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

                // Advanced rename options
                renameSeparator = props.getProperty("rename.separator", renameSeparator);
                renameAddDate = Boolean.parseBoolean(props.getProperty("rename.addDate", Boolean.toString(renameAddDate)));
                renameIncludeSubdirs = Boolean.parseBoolean(props.getProperty("rename.includeSubdirs", Boolean.toString(renameIncludeSubdirs)));
                renameUseSequential = Boolean.parseBoolean(props.getProperty("rename.useSequential", Boolean.toString(renameUseSequential)));
                renameIgnoreSystemFiles = Boolean.parseBoolean(props.getProperty("rename.ignoreSystemFiles", Boolean.toString(renameIgnoreSystemFiles)));
                renameDeleteIgnored = Boolean.parseBoolean(props.getProperty("rename.deleteIgnored", Boolean.toString(renameDeleteIgnored)));
                stratPrefixName = Boolean.parseBoolean(props.getProperty("rename.strat.prefixName", Boolean.toString(stratPrefixName)));
                stratPrefixNote = Boolean.parseBoolean(props.getProperty("rename.strat.prefixNote", Boolean.toString(stratPrefixNote)));
                stratSuffixName = Boolean.parseBoolean(props.getProperty("rename.strat.suffixName", Boolean.toString(stratSuffixName)));
                stratSuffixNote = Boolean.parseBoolean(props.getProperty("rename.strat.suffixNote", Boolean.toString(stratSuffixNote)));
                stratReplaceNote = Boolean.parseBoolean(props.getProperty("rename.strat.replaceNote", Boolean.toString(stratReplaceNote)));
                stratNoteNumber = Boolean.parseBoolean(props.getProperty("rename.strat.noteNumber", Boolean.toString(stratNoteNumber)));
                stratReplace = Boolean.parseBoolean(props.getProperty("rename.strat.replace", Boolean.toString(stratReplace)));
                stratSmartReplace = Boolean.parseBoolean(props.getProperty("rename.strat.smartReplace", Boolean.toString(stratSmartReplace)));
                stratCustom = Boolean.parseBoolean(props.getProperty("rename.strat.custom", Boolean.toString(stratCustom)));
                stratCustomFormat = props.getProperty("rename.strat.customFormat", stratCustomFormat);
                
                // Relink settings
                relinkByNote = Boolean.parseBoolean(props.getProperty("relink.byNote", Boolean.toString(relinkByNote)));
                relinkByTitle = Boolean.parseBoolean(props.getProperty("relink.byTitle", Boolean.toString(relinkByTitle)));
                relinkByTrimmed = Boolean.parseBoolean(props.getProperty("relink.byTrimmed", Boolean.toString(relinkByTrimmed)));
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
            // Advanced rename options
            props.setProperty("rename.separator", renameSeparator);
            props.setProperty("rename.addDate", Boolean.toString(renameAddDate));
            props.setProperty("rename.includeSubdirs", Boolean.toString(renameIncludeSubdirs));
            props.setProperty("rename.useSequential", Boolean.toString(renameUseSequential));
            props.setProperty("rename.ignoreSystemFiles", Boolean.toString(renameIgnoreSystemFiles));
            props.setProperty("rename.deleteIgnored", Boolean.toString(renameDeleteIgnored));
            props.setProperty("rename.strat.prefixName", Boolean.toString(stratPrefixName));
            props.setProperty("rename.strat.prefixNote", Boolean.toString(stratPrefixNote));
            props.setProperty("rename.strat.suffixName", Boolean.toString(stratSuffixName));
            props.setProperty("rename.strat.suffixNote", Boolean.toString(stratSuffixNote));
            props.setProperty("rename.strat.replaceNote", Boolean.toString(stratReplaceNote));
            props.setProperty("rename.strat.noteNumber", Boolean.toString(stratNoteNumber));
            props.setProperty("rename.strat.replace", Boolean.toString(stratReplace));
            props.setProperty("rename.strat.smartReplace", Boolean.toString(stratSmartReplace));
            props.setProperty("rename.strat.custom", Boolean.toString(stratCustom));
            props.setProperty("rename.strat.customFormat", stratCustomFormat);
            
            // Relink settings
            props.setProperty("relink.byNote", Boolean.toString(relinkByNote));
            props.setProperty("relink.byTitle", Boolean.toString(relinkByTitle));
            props.setProperty("relink.byTrimmed", Boolean.toString(relinkByTrimmed));
            
            try (FileOutputStream fos = new FileOutputStream(prefsFile)) {
                props.store(fos, "Digitizing Assistant Preferences");
            }
        } catch (IOException e) {
            System.err.println("Could not save preferences: " + e.getMessage());
        }
    }

    // Getters/setters for advanced rename options
    public String getRenameSeparator() { return renameSeparator; }
    public void setRenameSeparator(String v) { this.renameSeparator = v; savePreferences(); }
    public boolean isRenameAddDate() { return renameAddDate; }
    public void setRenameAddDate(boolean v) { this.renameAddDate = v; savePreferences(); }
    public boolean isRenameIncludeSubdirs() { return renameIncludeSubdirs; }
    public void setRenameIncludeSubdirs(boolean v) { this.renameIncludeSubdirs = v; savePreferences(); }
    public boolean isRenameUseSequential() { return renameUseSequential; }
    public void setRenameUseSequential(boolean v) { this.renameUseSequential = v; savePreferences(); }
    public boolean isRenameIgnoreSystemFiles() { return renameIgnoreSystemFiles; }
    public void setRenameIgnoreSystemFiles(boolean v) { this.renameIgnoreSystemFiles = v; savePreferences(); }
    public boolean isRenameDeleteIgnored() { return renameDeleteIgnored; }
    public void setRenameDeleteIgnored(boolean v) { this.renameDeleteIgnored = v; savePreferences(); }
    public boolean isStratPrefixName() { return stratPrefixName; }
    public void setStratPrefixName(boolean v) { this.stratPrefixName = v; savePreferences(); }
    public boolean isStratPrefixNote() { return stratPrefixNote; }
    public void setStratPrefixNote(boolean v) { this.stratPrefixNote = v; savePreferences(); }
    public boolean isStratSuffixName() { return stratSuffixName; }
    public void setStratSuffixName(boolean v) { this.stratSuffixName = v; savePreferences(); }
    public boolean isStratSuffixNote() { return stratSuffixNote; }
    public void setStratSuffixNote(boolean v) { this.stratSuffixNote = v; savePreferences(); }
    public boolean isStratReplaceNote() { return stratReplaceNote; }
    public void setStratReplaceNote(boolean v) { this.stratReplaceNote = v; savePreferences(); }
    public boolean isStratNoteNumber() { return stratNoteNumber; }
    public void setStratNoteNumber(boolean v) { this.stratNoteNumber = v; savePreferences(); }
    public boolean isStratReplace() { return stratReplace; }
    public void setStratReplace(boolean v) { this.stratReplace = v; savePreferences(); }
    public boolean isStratSmartReplace() { return stratSmartReplace; }
    public void setStratSmartReplace(boolean v) { this.stratSmartReplace = v; savePreferences(); }
    public boolean isStratCustom() { return stratCustom; }
    public void setStratCustom(boolean v) { this.stratCustom = v; savePreferences(); }
    public String getStratCustomFormat() { return stratCustomFormat; }
    public void setStratCustomFormat(String v) { this.stratCustomFormat = v; savePreferences(); }
    
    // Relink settings getters/setters
    public boolean isRelinkByNote() { return relinkByNote; }
    public void setRelinkByNote(boolean v) { this.relinkByNote = v; savePreferences(); }
    public boolean isRelinkByTitle() { return relinkByTitle; }
    public void setRelinkByTitle(boolean v) { this.relinkByTitle = v; savePreferences(); }
    public boolean isRelinkByTrimmed() { return relinkByTrimmed; }
    public void setRelinkByTrimmed(boolean v) { this.relinkByTrimmed = v; savePreferences(); }
} 