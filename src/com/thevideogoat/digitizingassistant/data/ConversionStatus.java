package com.thevideogoat.digitizingassistant.data;

import java.io.Serializable;

public enum ConversionStatus implements Serializable {

    NOT_STARTED("Not Started"),
    DAMAGED("Damaged"),
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
        ConversionStatus[] x = new ConversionStatus[5];
        x[0] = NOT_STARTED;
        x[2] = IN_PROGRESS;
        x[3] = BASIC_EDITING;
        x[4] = COMPLETED;
        x[1] = NOT_STARTED;
        return x;
    }
}
