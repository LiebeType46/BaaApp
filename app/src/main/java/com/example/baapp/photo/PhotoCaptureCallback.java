package com.example.baapp.photo;

import android.net.Uri;

public interface PhotoCaptureCallback {
    void onPhotoCaptured(Uri uri);
}