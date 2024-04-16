package com.psl.pallettracking.adapters;

public class DashboardModel {
    String menuSequence;
    String menuId;
    int drawableImage;

    public int getDrawableImage() {
        return drawableImage;
    }

    public void setDrawableImage(int drawableImage) {
        this.drawableImage = drawableImage;
    }

    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
    }

    public String getMenuSequence() {
        return menuSequence;
    }

    public void setMenuSequence(String menuSequence) {
        this.menuSequence = menuSequence;
    }

    public String getIsMenuActive() {
        return isMenuActive;
    }

    public void setIsMenuActive(String isMenuActive) {
        this.isMenuActive = isMenuActive;
    }

    String isMenuActive;
    String menuName;
    String menuActivityName;

    public String getMenuActivityName() {
        return menuActivityName;
    }

    public void setMenuActivityName(String menuActivityName) {
        this.menuActivityName = menuActivityName;
    }

    String menuimageName;

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getMenuimageName() {
        return menuimageName;
    }

    public void setMenuimageName(String menuimageName) {
        this.menuimageName = menuimageName;
    }


}
