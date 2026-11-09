package com.sharedfund.app.model;

import com.google.gson.annotations.SerializedName;
import com.sharedfund.app.R;

import java.io.Serializable;

public class ExpenseCategory implements Serializable {
    private String id;
    private String name;
    private String type;
    @SerializedName("icon_name")
    private String iconName;

    public ExpenseCategory() {
    }

    public ExpenseCategory(String id, String name, String type, String iconName) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.iconName = iconName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public int getIconDrawableRes() {
        if ("ic_restaurant".equalsIgnoreCase(iconName)) return R.drawable.ic_restaurant;
        if ("ic_shopping".equalsIgnoreCase(iconName)) return R.drawable.ic_shopping;
        if ("ic_home_category".equalsIgnoreCase(iconName)) return R.drawable.ic_home_category;
        if ("ic_transport".equalsIgnoreCase(iconName)) return R.drawable.ic_transport;
        if ("ic_wallet".equalsIgnoreCase(iconName)) return R.drawable.ic_wallet;
        if ("ic_sparkles".equalsIgnoreCase(iconName)) return R.drawable.ic_sparkles;
        return R.drawable.ic_receipt;
    }
}
