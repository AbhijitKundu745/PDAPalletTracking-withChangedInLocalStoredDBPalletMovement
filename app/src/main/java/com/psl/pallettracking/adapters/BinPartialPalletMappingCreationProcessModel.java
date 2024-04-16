package com.psl.pallettracking.adapters;

public class BinPartialPalletMappingCreationProcessModel {
    String binNumber,binDescription;
    String batchId;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    int pickedQty;

    public String getBinNumber() {
        return binNumber;
    }

    public void setBinNumber(String binNumber) {
        this.binNumber = binNumber;
    }

    public String getBinDescription() {
        return binDescription;
    }

    public void setBinDescription(String binDescription) {
        this.binDescription = binDescription;
    }

    public int getPickedQty() {
        return pickedQty;
    }

    public void setPickedQty(int pickedQty) {
        this.pickedQty = pickedQty;
    }
}
