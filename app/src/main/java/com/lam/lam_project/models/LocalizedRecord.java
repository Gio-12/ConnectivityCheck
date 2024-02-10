package com.lam.lam_project.models;

import java.io.Serializable;

public class LocalizedRecord implements Serializable {
    Record basicRecord;
    String mgrsCoords;
    String timestamp;

    public LocalizedRecord(Record record, String mgrsCoords, String timestamp) {
        this.basicRecord = record;
        this.mgrsCoords = mgrsCoords;
        this.timestamp = timestamp;
    }

    public Record getBasicRecord() {
        return basicRecord;
    }

    public void setBasicRecord(Record basicRecord) {
        this.basicRecord = basicRecord;
    }

    public String getMgrsCoords() {
        return mgrsCoords;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setMgrsCoords(String mgrsCoords) {
        this.mgrsCoords = mgrsCoords;
    }
}
