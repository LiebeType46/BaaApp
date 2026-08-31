package org.baanet.baaapp.sync;

public class LocationPhotoUploadResponse {

    public String resCode;
    public Long serverLocationId;
    public String photoPath;

    public boolean isOk() {
        return "OK".equals(resCode);
    }
}
