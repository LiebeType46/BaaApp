package org.baanet.baaapp.sync;

import java.util.List;

public class LocationSyncRequest {

    public List<LocationUploadRequest> locations;

    public LocationSyncRequest(List<LocationUploadRequest> locations) {
        this.locations = locations;
    }
}
