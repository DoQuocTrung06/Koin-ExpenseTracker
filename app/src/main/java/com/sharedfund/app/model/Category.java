package com.sharedfund.app.model;

import com.google.gson.annotations.SerializedName;

public class Category {
    @SerializedName("id") private int id;
    @SerializedName("name") private String name;
    @SerializedName("type") private String type;
    @SerializedName("icon_name") private String iconName;
    @SerializedName("color_code") private String colorCode;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getIconName() { return iconName; }
    public String getColorCode() { return colorCode; }
}
