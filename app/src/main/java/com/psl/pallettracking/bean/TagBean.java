package com.psl.pallettracking.bean;

import java.util.Objects;

public class TagBean {
    private String batchId;
    private String epcId;
    private int rssi;
    private int times;
    private String antenna;
    private String additionalData;
    private String tagType;
    private String addedDateTime;

    public TagBean(String batchId, String epcId, int rssi, int times, String antenna, String additionalData, String tagType, String addedDateTime) {
        this.batchId = batchId;
        this.epcId = epcId;
        this.rssi = rssi;
        this.times = times;
        this.antenna = antenna;
        this.additionalData = additionalData;
        this.tagType = tagType;
        this.addedDateTime = addedDateTime;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getEpcId() {
        return epcId;
    }

    public void setEpcId(String epcId) {
        this.epcId = epcId;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public String getAntenna() {
        return antenna;
    }

    public void setAntenna(String antenna) {
        this.antenna = antenna;
    }

    public String getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(String additionalData) {
        this.additionalData = additionalData;
    }

    public String getTagType() {
        return tagType;
    }

    public void setTagType(String tagType) {
        this.tagType = tagType;
    }

    public String getAddedDateTime() {
        return addedDateTime;
    }

    public void setAddedDateTime(String addedDateTime) {
        this.addedDateTime = addedDateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagBean tagBean = (TagBean) o;
        return rssi == tagBean.rssi && times == tagBean.times && Objects.equals(batchId, tagBean.batchId) && Objects.equals(epcId, tagBean.epcId) && Objects.equals(antenna, tagBean.antenna) && Objects.equals(additionalData, tagBean.additionalData) && Objects.equals(tagType, tagBean.tagType) && Objects.equals(addedDateTime, tagBean.addedDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId, epcId, rssi, times, antenna, additionalData, tagType, addedDateTime);
    }

    @Override
    public String toString() {
        return "TagBean{" +
                "batchId='" + batchId + '\'' +
                ", epcId='" + epcId + '\'' +
                ", rssi=" + rssi +
                ", times=" + times +
                ", antenna='" + antenna + '\'' +
                ", additionalData='" + additionalData + '\'' +
                ", tagType='" + tagType + '\'' +
                ", addedDateTime='" + addedDateTime + '\'' +
                '}';
    }
}
