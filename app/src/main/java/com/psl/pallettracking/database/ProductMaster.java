package com.psl.pallettracking.database;

public class ProductMaster {
    String productTagId,productName,productType;

    public String getProductTagId() {
        return productTagId;
    }

    public void setProductTagId(String productTagId) {
        this.productTagId = productTagId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }
}
