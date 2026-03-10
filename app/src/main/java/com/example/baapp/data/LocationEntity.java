package com.example.baapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "locations")
public class LocationEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String category;
    private String subCategory;
    private double latitude;
    private double longitude;
    private String timestamp;
    private String memo;
    private boolean uploadFlg;


    public LocationEntity(String category, String subCategory, double latitude, double longitude, String timestamp, String memo, boolean uploadFlg, String photoUri) {
        this.category = category;
        this.subCategory = subCategory;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.memo = memo;
        this.uploadFlg = uploadFlg;
        this.photoUri = photoUri;
    }

    // Getter / Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public boolean isUploadFlg() {
        return uploadFlg;
    }

    public void setUploadFlg(boolean uploadFlg) {
        this.uploadFlg = uploadFlg;
    }


    private String photoUri;

    public String getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }

}

