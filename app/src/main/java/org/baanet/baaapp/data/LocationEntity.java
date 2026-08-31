package org.baanet.baaapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.baanet.baaapp.common.MainCategoryConverter;
import org.baanet.baaapp.photo.PhotoService;

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
    private String ownerPublicId;
    private Long serverLocationId;
    private boolean photoUploadFlg;
    private int photoUploadRetryCount;
    private String lastPhotoUploadError;


    public LocationEntity(String category, String subCategory, double latitude, double longitude, String timestamp, String memo, boolean uploadFlg, String photoUri) {
        setCategory(category);
        this.subCategory = subCategory;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.memo = memo;
        this.uploadFlg = uploadFlg;
        setPhotoUri(photoUri);
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
        this.category = MainCategoryConverter.normalizeCategoryIdOrDefault(category);
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

    public String getOwnerPublicId() {
        return ownerPublicId;
    }

    public void setOwnerPublicId(String ownerPublicId) {
        this.ownerPublicId = normalizeOwnerPublicId(ownerPublicId);
    }

    public Long getServerLocationId() {
        return serverLocationId;
    }

    public void setServerLocationId(Long serverLocationId) {
        this.serverLocationId = serverLocationId;
    }

    public boolean isPhotoUploadFlg() {
        return photoUploadFlg;
    }

    public void setPhotoUploadFlg(boolean photoUploadFlg) {
        this.photoUploadFlg = photoUploadFlg;
    }

    public int getPhotoUploadRetryCount() {
        return photoUploadRetryCount;
    }

    public void setPhotoUploadRetryCount(int photoUploadRetryCount) {
        this.photoUploadRetryCount = Math.max(photoUploadRetryCount, 0);
    }

    public String getLastPhotoUploadError() {
        return lastPhotoUploadError;
    }

    public void setLastPhotoUploadError(String lastPhotoUploadError) {
        this.lastPhotoUploadError = lastPhotoUploadError;
    }

    private String normalizeOwnerPublicId(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String photoUri;

    public String getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(String photoUri) {
        this.photoUri = PhotoService.normalizeStoredPhotoPath(photoUri);
    }

}

