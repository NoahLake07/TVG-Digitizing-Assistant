package com.thevideogoat.digitizingassistant.data;

public enum Type {

    VHS("VHS"),
    VHSC("VHS-C"),
    _8MM("8mm"),
    BETAMAX("Betamax"),
    MINIDV("MiniDV"),
    CD_DVD("CD/DVD"),
    TYPE_II("Type II"),
    MISC_DATA_STORAGE("Misc Data Storage"),
    ;

    private final String displayName;
    Type(String displayName){
        this.displayName = displayName;
    }

    @Override
    public String toString(){
        return this.displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Type fromDisplayName(String name) {
        for (Type t : values()) {
            if (t.displayName.equalsIgnoreCase(name) || t.name().equalsIgnoreCase(name.replace("/", "_"))) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown type: " + name);
    }
}
