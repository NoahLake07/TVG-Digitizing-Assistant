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

    public String getDay(){
        return this.day;
    }

    public String getMonth(){
        return this.month;
    }

    public String getYear(){
        return this.year;
    }
}