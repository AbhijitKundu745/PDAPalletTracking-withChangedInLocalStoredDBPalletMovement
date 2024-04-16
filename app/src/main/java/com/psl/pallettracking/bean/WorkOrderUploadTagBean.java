package com.psl.pallettracking.bean;

import java.util.Objects;

public class WorkOrderUploadTagBean {
    private String batchId;
    private String epcId;
    private String workOrderNumber;
    private String workOrderType;
    private int rssi;
    private int times;
    private String antenna;
    private String additionalData;
    private String tagType;
    private String addedDateTime;

    public WorkOrderUploadTagBean(String batchId, String epcId, String workOrderNumber, String workOrderType, int rssi, int times, String antenna, String additionalData, String tagType, String addedDateTime) {
        this.batchId = batchId;
        this.epcId = epcId;
        this.workOrderNumber = workOrderNumber;
        this.workOrderType = workOrderType;
        this.rssi = rssi;
        this.times = times;
        this.antenna = antenna;
        this.additionalData = additionalData;
        this.tagType = tagType;
        this.addedDateTime = addedDateTime;
    }

    public WorkOrderUploadTagBean(TagBean tagBean, String workOrderNumber, String workOrderType) {
        this(tagBean.getBatchId(), tagBean.getEpcId(), workOrderNumber, workOrderType, tagBean.getRssi(), tagBean.getTimes(), tagBean.getAntenna(), tagBean.getAdditionalData(), tagBean.getTagType(), tagBean.getAddedDateTime());
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

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }
    public String getWorkOrderType() {
        return workOrderType;
    }

    public void setWorkOrderType(String workOrderType) {
        this.workOrderType = workOrderType;
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
        WorkOrderUploadTagBean that = (WorkOrderUploadTagBean) o;
        return rssi == that.rssi && times == that.times && Objects.equals(batchId, that.batchId) && Objects.equals(epcId, that.epcId) && Objects.equals(workOrderNumber, that.workOrderNumber) && Objects.equals(antenna, that.antenna) && Objects.equals(additionalData, that.additionalData) && Objects.equals(tagType, that.tagType) && Objects.equals(addedDateTime, that.addedDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId, epcId, workOrderNumber, rssi, times, antenna, additionalData, tagType, addedDateTime);
    }

    @Override
    public String toString() {
        return "WorkOrderUploadTagBean{" +
                "batchId='" + batchId + '\'' +
                ", epcId='" + epcId + '\'' +
                ", workOrderNumber='" + workOrderNumber + '\'' +
                ", rssi=" + rssi +
                ", times=" + times +
                ", antenna='" + antenna + '\'' +
                ", additionalData='" + additionalData + '\'' +
                ", tagType='" + tagType + '\'' +
                ", addedDateTime='" + addedDateTime + '\'' +
                '}';
    }
}
