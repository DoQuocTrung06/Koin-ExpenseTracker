package com.sharedfund.app.model;

import com.google.gson.annotations.SerializedName;

public class Category {
    @SerializedName("id") private int id;
    @SerializedName("name") private String name;
    @SerializedName("type") private String type;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
}
