package com.thevideogoat.digitizingassistant.data;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

public class Conversion implements Serializable {

    public String name, note;
    public Type type;
    public ArrayList<File> linkedFiles;
    public Date dateOfConversion;
    public Time timeOfConversion;

    public Conversion(String name){
        // assign name
        this.name = name;

        // assign default values
        this.note = "";
        this.type = Type.VHS;
        this.linkedFiles = new ArrayList<>();
        dateOfConversion = new Date();
        timeOfConversion = new Time();
    }

}
