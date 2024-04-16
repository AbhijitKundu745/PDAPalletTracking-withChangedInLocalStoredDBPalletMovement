package com.psl.pallettracking.adapters;

public class BinPartialPalletMappingAdapterModel {
    String workOrdernumber,workOrderStatus, DRN;

    public String getWorkOrdernumber() {
        return workOrdernumber;
    }

    public void setWorkOrdernumber(String workOrdernumber) {
        this.workOrdernumber = workOrdernumber;
    }

    public String getWorkOrderStatus() {
        return workOrderStatus;
    }

    public void setWorkOrderStatus(String workOrderStatus) {
        this.workOrderStatus = workOrderStatus;
    }
    public String getDRN() {
        return DRN;
    }

    public void setDRN(String DRN) {
        this.DRN = DRN;
    }
}
