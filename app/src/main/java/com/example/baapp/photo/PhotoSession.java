package com.example.baapp.photo;

import android.net.Uri;

public class PhotoSession {
    private static final PhotoSession instance = new PhotoSession();

    private Uri photoUri;
    private PhotoCaptureCallback callback;

    private PhotoSession() {}

    public static PhotoSession getInstance() {
        return instance;
    }

    public Uri getPhotoUri() { return photoUri; }
    public void setPhotoUri(Uri uri) { this.photoUri = uri; }

    public PhotoCaptureCallback getCallback() { return callback; }
    public void setCallback(PhotoCaptureCallback callback) { this.callback = callback; }

    public void clear() {
        photoUri = null;
        callback = null;
    }
}
