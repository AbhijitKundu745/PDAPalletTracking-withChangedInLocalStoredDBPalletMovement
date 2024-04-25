package com.psl.pallettracking.bean;

public class TagWithDestination {
    private String batchID;
    private String WorkOrderNo;
    private String palletTag;
    private String destinationTag;
    private String WorkOrderType;
    private String dateTime;

    public TagWithDestination(String batchID, String WorkOrderNo, String palletTag, String destination, String workorderType, String dateTime) {
        this.batchID = batchID;
        this.WorkOrderNo = WorkOrderNo;
        this.palletTag = palletTag;
        this.destinationTag = destination;
        this.WorkOrderType = workorderType;
        this.dateTime = dateTime;
    }
    public String getBatchID() {
        return batchID;
    }

    public void setBatchID(String batchID) {
        this.batchID = batchID;
    }
    public String getWorkOrderNo() {
        return WorkOrderNo;
    }

    public void setWorkOrderNo(String WorkOrderNo) {
        this.WorkOrderNo = WorkOrderNo;
    }
    public String getPalletTag() {
        return palletTag;
    }

    public void setPalletTag(String palletTag) {
        this.palletTag = palletTag;
    }

    public String getDestinationTag() {
        return destinationTag;
    }

    public void setDestinationTag(String destination) {
        this.destinationTag = destination;
    }

    public String getWorkOrderType() {
        return WorkOrderType;
    }

    public void setWorkOrderType(String WorkOrderType) {
        this.WorkOrderType = WorkOrderType;
    }
    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}
