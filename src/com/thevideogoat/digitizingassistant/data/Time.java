package com.thevideogoat.digitizingassistant.data;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Time implements Serializable {
    public String hour, minute, am_pm;

    public Time(){
        LocalTime now = LocalTime.now();
        int hour24 = now.getHour();
        this.am_pm = hour24 < 12 ? "AM" : "PM";
        int hour12 = hour24 <= 12 ? hour24 : hour24 - 12;
        this.hour = hour12 == 0 ? "12" : String.valueOf(hour12); // convert 0 to 12 for 12 AM
        this.minute = String.valueOf(now.getMinute());
    }

    public Time(String hour, String minute, String am_pm) {
        this.hour = hour;
        this.minute = minute;
        this.am_pm = am_pm;
    }

    public String getHour(){
        return this.hour;
    }

    public String getMinute(){
        return this.minute;
    }

    public String getAmPm(){
        return this.am_pm;
    }

    @Override
    public String toString() {
        return String.format("%s:%s %s", hour, minute, am_pm);
    }
}