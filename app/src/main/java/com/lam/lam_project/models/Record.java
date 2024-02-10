package com.lam.lam_project.models;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Record implements Comparable<Record>, Serializable {
    private RecordType type;
    private String timeStamp;
    private int condition;
    private double value;

    public Record(RecordType type, int condition, double value) {
        this.type = type;
        this.value = value;
        this.condition = condition;
        this.timeStamp = calculateTimeStamp();
    }

    public Record(RecordType type, int condition, double value, String timeStamp) {
        this.type = type;
        this.value = value;
        this.condition = condition;
        this.timeStamp = timeStamp;
    }

    private String calculateTimeStamp(){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }

    public RecordType getType() {
        return type;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public int getCondition() {
        return condition;
    }

    public double getValue() {
        return value;
    }

    public void setType(RecordType type) {
        this.type = type;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setCondition(int condition) {
        this.condition = condition;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Record {" +
                "\ntype=" + type +
                ", \ntimeStamp=" + timeStamp +
                ", \ncondition=" + condition +
                ", \nvalue=" + value +
                "\n}";
    }

    @Override
    public int compareTo(Record s) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date thisDate = dateFormat.parse(this.getTimeStamp());
            Date otherDate = dateFormat.parse(s.getTimeStamp());

            return thisDate.compareTo(otherDate);
        } catch (ParseException e) {
            System.out.println("Error date conversion");
        }
        return 0;
    }
}
