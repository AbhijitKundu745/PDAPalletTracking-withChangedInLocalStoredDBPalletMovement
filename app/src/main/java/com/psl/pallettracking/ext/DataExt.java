package com.psl.pallettracking.ext;

public class DataExt {
    public static String getTagType(String str) {
        if (str.length() > 4) {
            String sub = str.substring(2, 4);
            switch (sub) {
                case "02":
                    return typePallet;
                case "03":
                    return typeBean;
                case "04":
                    return typeTemporaryStorage;
                case "05":
                    return typeLoadingArea;
            }
        }
        return typeOther;
    }

    public static final String typePallet = "PalletTag";
    public static final String typeBean = "Bintag";
    public static final String typeTemporaryStorage = "TS";
    public static final String typeLoadingArea = "LA";
    public static final String typeOther = "NA";

    public static String getCategoryID(String str) {
        if (str.length() > 4) {
            String sub = str.substring(2, 4);
            switch (sub) {
                case "02":
                    return categoryPallet;
                case "03":
                    return categoryBean;
                case "04":
                    return categoryTemporaryStorage;
                case "05":
                    return categoryLoadingArea;
            }
        }
        return categoryOther;
    }

    public static final String categoryPallet = "2";
    public static final String categoryBean = "3";
    public static final String categoryTemporaryStorage = "4";
    public static final String categoryLoadingArea = "5";
    public static final String categoryOther = "0";
}
