package com.thevideogoat.digitizingassistant.data;

import com.thevideogoat.digitizingassistant.ui.DigitizingAssistant;

import java.awt.*;
import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Conversion implements Serializable {

    @Serial
    private static final long serialVersionUID = 7899114141134424890L;
    public String name, note, technicianNotes;
    public Type type;
    public ArrayList<FileReference> linkedFiles;
    public Date dateOfConversion;
    public Time timeOfConversion;
    public ConversionStatus status;
    public String version;
    public Duration duration = Duration.ZERO;
    public boolean isDataOnly = false;
    public List<DamageEvent> damageHistory = new ArrayList<>();

    public Conversion(String name){
        // assign name
        this.name = name;

        // assign default values
        this.note = "";
        this.technicianNotes = "";
        this.type = Type.VHS;
        this.linkedFiles = new ArrayList<>();
        dateOfConversion = new Date();
        timeOfConversion = new Time();
        duration = Duration.ZERO;
        status = ConversionStatus.NOT_STARTED;
        version = DigitizingAssistant.VERSION;
        isDataOnly = false;
        damageHistory = new ArrayList<>();
    }

    @Serial
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        if (status == null) {
            status = ConversionStatus.NOT_STARTED;
        }
        // Handle legacy conversions that don't have the new fields
        if (technicianNotes == null) {
            technicianNotes = "";
        }
        // isDataOnly defaults to false for legacy conversions
        if (damageHistory == null) {
            damageHistory = new ArrayList<>();
        }
    }

    public Color getStatusColor() {
        switch (status) {
            case NOT_STARTED:
                return Color.DARK_GRAY;
            case DAMAGED:
                return Color.RED;
            case DAMAGE_FIXED:
                return new Color(255, 165, 0); // Orange - fixed damage
            case DAMAGE_IRREVERSIBLE:
                return new Color(128, 0, 128); // Purple - irreversible damage
            case IN_PROGRESS:
                return Color.ORANGE;
            case BASIC_EDITING:
                return Color.BLUE;
            case COMPLETED:
                return Color.GREEN;
            default:
                return Color.BLACK;
        }
    }

    public void addDamageEvent(String description, String technicianNotes) {
        DamageEvent event = new DamageEvent(description, technicianNotes);
        damageHistory.add(event);
    }

    public static class DamageEvent implements Serializable {
        public LocalDateTime timestamp;
        public String description;
        public String technicianNotes;

        public DamageEvent(String description, String technicianNotes) {
            this.timestamp = LocalDateTime.now();
            this.description = description;
            this.technicianNotes = technicianNotes;
        }
    }
}
