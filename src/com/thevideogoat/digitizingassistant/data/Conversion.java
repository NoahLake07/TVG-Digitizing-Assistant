package com.thevideogoat.digitizingassistant.data;

import com.thevideogoat.digitizingassistant.ui.DigitizingAssistant;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;

public class Conversion implements Serializable {

    @Serial
    private static final long serialVersionUID = 7899114141134424890L;
    public String name, note;
    public Type type;
    public ArrayList<File> linkedFiles;
    public Date dateOfConversion;
    public Time timeOfConversion;
    public ConversionStatus status;
    public String version;
    public Duration duration = Duration.ZERO;

    public Conversion(String name){
        // assign name
        this.name = name;

        // assign default values
        this.note = "";
        this.type = Type.VHS;
        this.linkedFiles = new ArrayList<>();
        dateOfConversion = new Date();
        timeOfConversion = new Time();
        duration = Duration.ZERO;
        status = ConversionStatus.NOT_STARTED;
        version = DigitizingAssistant.VERSION;
    }

    @Serial
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        if (status == null) {
            status = ConversionStatus.NOT_STARTED;
        }
    }

}
