package org.baanet.baaapp.sync;

import org.baanet.baaapp.data.LocationEntity;

public class LocationUploadRequest {

    public int localId;
    public String category;
    public String subCategory;
    public double latitude;
    public double longitude;
    public String timestamp;
    public String memo;
    public String photoUri;

    public static LocationUploadRequest fromEntity(LocationEntity entity) {
        LocationUploadRequest request = new LocationUploadRequest();
        request.localId = entity.getId();
        request.category = entity.getCategory();
        request.subCategory = entity.getSubCategory();
        request.latitude = entity.getLatitude();
        request.longitude = entity.getLongitude();
        request.timestamp = entity.getTimestamp();
        request.memo = entity.getMemo();
        request.photoUri = entity.getPhotoUri();
        return request;
    }
}
