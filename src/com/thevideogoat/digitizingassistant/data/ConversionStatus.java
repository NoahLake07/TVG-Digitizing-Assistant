package com.thevideogoat.digitizingassistant.data;

import java.io.Serializable;

public enum ConversionStatus implements Serializable {

    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    BASIC_EDITING("Basic Editing"),
    COMPLETED("Completed"),
    DAMAGED("Damaged"),
    DAMAGE_FIXED("Damage Fixed"),
    DAMAGE_IRREVERSIBLE("Damage Irreversible"),
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
        return ConversionStatus.values();
    }
}
