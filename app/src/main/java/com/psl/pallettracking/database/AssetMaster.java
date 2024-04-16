package com.psl.pallettracking.database;

public class AssetMaster {
    String assetId,assetTypeId,assetTypeName,assetName,isAssetActive,assetSerialNumber;
    String isAssetRegistered;

    public String getIsAssetRegistered() {
        return isAssetRegistered;
    }

    public void setIsAssetRegistered(String isAssetRegistered) {
        this.isAssetRegistered = isAssetRegistered;
    }

    public String getAssetTypeId() {
        return assetTypeId;
    }

    public void setAssetTypeId(String assetTypeId) {
        this.assetTypeId = assetTypeId;
    }

    public String getAssetTypeName() {
        return assetTypeName;
    }

    public void setAssetTypeName(String assetTypeName) {
        this.assetTypeName = assetTypeName;
    }

    public String getAssetSerialNumber() {
        return assetSerialNumber;
    }

    public void setAssetSerialNumber(String assetSerialNumber) {
        this.assetSerialNumber = assetSerialNumber;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getIsAssetActive() {
        return isAssetActive;
    }

    public void setIsAssetActive(String isAssetActive) {
        this.isAssetActive = isAssetActive;
    }
}
