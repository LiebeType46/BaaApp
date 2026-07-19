package org.baanet.baaapp.photo;

import android.net.Uri;

public interface PhotoCaptureCallback {
    void onPhotoCaptured(Uri uri);
}