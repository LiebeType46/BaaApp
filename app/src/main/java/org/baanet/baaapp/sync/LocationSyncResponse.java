package org.baanet.baaapp.sync;

import java.util.ArrayList;
import java.util.List;

public class LocationSyncResponse {

    public String resCode;
    public List<Integer> uploadedLocalIds = new ArrayList<>();

    public boolean isOk() {
        return "OK".equals(resCode);
    }
}
