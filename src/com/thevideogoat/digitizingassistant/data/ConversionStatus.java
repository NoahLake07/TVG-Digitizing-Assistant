package com.thevideogoat.digitizingassistant.data;

import java.io.Serializable;

public enum ConversionStatus implements Serializable {

    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    BASIC_EDITING("Basic Editing"),
    COMPLETED("Completed"),
    ;

    final String name;
    ConversionStatus(String s) {
        this.name = s;
    }

    @Override
    public String toString(){
        return this.name;
    }

    public ConversionStatus[] getValues(){
        ConversionStatus[] x = new ConversionStatus[4];
        x[0] = NOT_STARTED;
        x[1] = IN_PROGRESS;
        x[2] = BASIC_EDITING;
        x[3] = COMPLETED;
        return x;
    }
}
