package com.thevideogoat.digitizingassistant.data;

import java.time.LocalDate;

public class Date implements java.io.Serializable {
    public String day, month, year;

    public Date(){
        LocalDate today = LocalDate.now();
        this.day = String.valueOf(today.getDayOfMonth());
        this.month = String.valueOf(today.getMonthValue());
        this.year = String.valueOf(today.getYear());
    }

    public Date(String day, String month, String year) {
        this.day = day;
        this.month = month;
        this.year = year;
    }

    public String getDay(){
        return this.day;
    }

    public String getMonth(){
        return this.month;
    }

    public String getYear(){
        return this.year;
    }

    @Override
    public String toString() {
        return String.format("%s/%s/%s", month, day, year);
    }
}