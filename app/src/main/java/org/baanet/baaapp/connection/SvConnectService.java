package org.baanet.baaapp.connection;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.baanet.baaapp.api.LocationSyncApi;
import org.baanet.baaapp.common.LanguageService;
import org.baanet.baaapp.data.AppDatabase;
import org.baanet.baaapp.data.LocationEntity;
import org.baanet.baaapp.common.UserDataScope;
import org.baanet.baaapp.sync.LocationSyncRequest;
import org.baanet.baaapp.sync.LocationSyncResponse;
import org.baanet.baaapp.sync.LocationUploadRequest;
import org.baanet.baaapp.sync.UploadedLocationResponse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SvConnectService {
    private static final String TAG = "BaaSync";
    private static final String PREF = "baa_prefs";
    private static final String KEY_TOKEN = "token";
    private static final int PHOTO_UPLOAD_MAX_ATTEMPTS = 3;
    private static final long PHOTO_UPLOAD_RETRY_DELAY_MS = 15_000L;

    public interface UploadCallback {
        void onComplete(int uploadedCount, int photoUploadedCount, int photoFailedCount);
        void onNoTarget();
        void onError(String message);
    }

    public static void upload(Context context) {
        LanguageService language = LanguageService.get(context);
        upload(context, new UploadCallback() {
            @Override
            public void onComplete(int uploadedCount, int photoUploadedCount, int photoFailedCount) {
                Toast.makeText(
                        context,
                        language.format("sync.complete", uploadedCount, photoUploadedCount, photoFailedCount),
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onNoTarget() {
                Toast.makeText(context, language.t("sync.no_target"), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void upload(Context context, UploadCallback callback) {
        Context appContext = context.getApplicationContext();
        LanguageService language = LanguageService.get(appContext);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        AppDatabase db = AppDatabase.getInstance(appContext);

        SharedPreferences prefs = appContext.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, null);
        Log.d(TAG, "Upload requested tokenPresent=" + (token != null && !token.isBlank())
                + ", tokenLength=" + (token != null ? token.length() : 0));
        if (token == null || token.isBlank()) {
            Log.w(TAG, "Upload aborted: token is empty");
            callback.onError(language.t("sync.no_login"));
            return;
        }

        // 未送信、または写真同期に必要なサーバーIDを持たないデータを先に同期する
        String ownerPublicId = UserDataScope.getCurrentPublicId(appContext);
        Log.d(TAG, "Upload ownerPublicId=" + ownerPublicId);
        List<LocationEntity> unuploadedLocations = ownerPublicId != null
                ? db.locationDao().getLocationsNeedingServerSyncByOwner(ownerPublicId)
                : db.locationDao().getUnownedLocationsNeedingServerSync();
        Log.d(TAG, "Server sync target count=" + unuploadedLocations.size());

        if (unuploadedLocations.isEmpty()) {
            Log.d(TAG, "Upload skipped: no target locations, checking pending photos");
            processPendingPhotos(appContext, db, token, ownerPublicId, mainHandler, callback, language, 0);
            return;
        }

        List<LocationUploadRequest> uploadRequests = new ArrayList<>();
        for (LocationEntity location : unuploadedLocations) {
            uploadRequests.add(LocationUploadRequest.fromEntity(location));
            Log.d(TAG, "Upload target localId=" + location.getId()
                    + ", category=" + location.getCategory()
                    + ", timestamp=" + location.getTimestamp()
                    + ", hasPhoto=" + (location.getPhotoUri() != null && !location.getPhotoUri().isBlank()));
        }

        LocationSyncApi.sync(token, new LocationSyncRequest(uploadRequests), new LocationSyncApi.LocationSyncCallback() {
            @Override
            public void onSuccess(LocationSyncResponse response) {
                mainHandler.post(() -> {
                    if (response == null || !response.isOk() || response.uploadedLocations == null) {
                        Log.w(TAG, "Upload failed: invalid success response");
                        callback.onError(language.format("sync.failed", ""));
                        return;
                    }

                    Log.d(TAG, "Upload succeeded uploadedLocations=" + response.uploadedLocations.size());
                    for (UploadedLocationResponse uploadedLocation : response.uploadedLocations) {
                        if (uploadedLocation.serverLocationId != null) {
                            db.locationDao().markLocationUploaded(
                                    uploadedLocation.localId,
                                    uploadedLocation.serverLocationId
                            );
                        } else {
                            Log.w(TAG, "Upload response missing serverLocationId localId=" + uploadedLocation.localId);
                        }
                    }
                    processPendingPhotos(
                            appContext,
                            db,
                            token,
                            ownerPublicId,
                            mainHandler,
                            callback,
                            language,
                            response.uploadedLocations.size()
                    );
                });
            }

            @Override
            public void onError(int httpCode, String responseBody) {
                Log.w(TAG, "Upload http error code=" + httpCode + ", body=" + responseBody);
                mainHandler.post(() ->
                        callback.onError(language.format("sync.failed", httpCode + " " + responseBody))
                );
            }

            @Override
            public void onFailure(IOException e) {
                Log.e(TAG, "Upload network failure", e);
                mainHandler.post(() ->
                        callback.onError(language.format("sync.network_error", e.getMessage()))
                );
            }
        });
    }

    private static void processPendingPhotos(
            Context appContext,
            AppDatabase db,
            String token,
            String ownerPublicId,
            Handler mainHandler,
            UploadCallback callback,
            LanguageService language,
            int uploadedLocationCount
    ) {
        List<LocationEntity> pendingPhotos = ownerPublicId != null
                ? db.locationDao().getPendingPhotoUploadsByOwner(ownerPublicId)
                : db.locationDao().getUnownedPendingPhotoUploads();

        Log.d(TAG, "Pending photo count=" + pendingPhotos.size());
        if (pendingPhotos.isEmpty()) {
            if (uploadedLocationCount == 0) {
                callback.onNoTarget();
            } else {
                callback.onComplete(uploadedLocationCount, 0, 0);
            }
            return;
        }

        PhotoUploadSummary summary = new PhotoUploadSummary();
        uploadNextPhoto(appContext, db, token, pendingPhotos, 0, 1, summary, mainHandler, callback, language, uploadedLocationCount);
    }

    private static void uploadNextPhoto(
            Context appContext,
            AppDatabase db,
            String token,
            List<LocationEntity> pendingPhotos,
            int index,
            int attempt,
            PhotoUploadSummary summary,
            Handler mainHandler,
            UploadCallback callback,
            LanguageService language,
            int uploadedLocationCount
    ) {
        if (index >= pendingPhotos.size()) {
            Log.d(TAG, "Photo upload finished success=" + summary.successCount + ", failed=" + summary.failedCount);
            callback.onComplete(uploadedLocationCount, summary.successCount, summary.failedCount);
            return;
        }

        LocationEntity location = pendingPhotos.get(index);
        Long serverLocationId = location.getServerLocationId();
        File photoFile = location.getPhotoUri() != null
                ? new File(appContext.getFilesDir(), location.getPhotoUri())
                : null;

        if (serverLocationId == null || photoFile == null || !photoFile.exists()) {
            String error = "photo file missing localId=" + location.getId()
                    + ", serverLocationId=" + serverLocationId
                    + ", photoUri=" + location.getPhotoUri();
            Log.w(TAG, error);
            db.locationDao().markPhotoUploadFailed(location.getId(), error);
            summary.failedCount++;
            uploadNextPhoto(appContext, db, token, pendingPhotos, index + 1, 1, summary, mainHandler, callback, language, uploadedLocationCount);
            return;
        }

        Log.d(TAG, "Photo upload start localId=" + location.getId()
                + ", serverLocationId=" + serverLocationId
                + ", attempt=" + attempt + "/" + PHOTO_UPLOAD_MAX_ATTEMPTS);

        LocationSyncApi.uploadPhoto(token, serverLocationId, photoFile, new LocationSyncApi.PhotoUploadCallback() {
            @Override
            public void onSuccess(String responseBody) {
                mainHandler.post(() -> {
                    Log.d(TAG, "Photo upload succeeded localId=" + location.getId()
                            + ", serverLocationId=" + serverLocationId);
                    db.locationDao().markPhotoUploaded(location.getId());
                    summary.successCount++;
                    uploadNextPhoto(appContext, db, token, pendingPhotos, index + 1, 1, summary, mainHandler, callback, language, uploadedLocationCount);
                });
            }

            @Override
            public void onError(int httpCode, String responseBody) {
                mainHandler.post(() -> retryOrCarryPhoto(
                        appContext,
                        db,
                        token,
                        pendingPhotos,
                        index,
                        attempt,
                        summary,
                        mainHandler,
                        callback,
                        language,
                        uploadedLocationCount,
                        location,
                        "HTTP " + httpCode + " " + responseBody
                ));
            }

            @Override
            public void onFailure(IOException e) {
                mainHandler.post(() -> retryOrCarryPhoto(
                        appContext,
                        db,
                        token,
                        pendingPhotos,
                        index,
                        attempt,
                        summary,
                        mainHandler,
                        callback,
                        language,
                        uploadedLocationCount,
                        location,
                        e.getMessage()
                ));
            }
        });
    }

    private static void retryOrCarryPhoto(
            Context appContext,
            AppDatabase db,
            String token,
            List<LocationEntity> pendingPhotos,
            int index,
            int attempt,
            PhotoUploadSummary summary,
            Handler mainHandler,
            UploadCallback callback,
            LanguageService language,
            int uploadedLocationCount,
            LocationEntity location,
            String error
    ) {
        Log.w(TAG, "Photo upload failed localId=" + location.getId()
                + ", attempt=" + attempt + "/" + PHOTO_UPLOAD_MAX_ATTEMPTS
                + ", error=" + error);

        if (attempt < PHOTO_UPLOAD_MAX_ATTEMPTS) {
            mainHandler.postDelayed(
                    () -> uploadNextPhoto(
                            appContext,
                            db,
                            token,
                            pendingPhotos,
                            index,
                            attempt + 1,
                            summary,
                            mainHandler,
                            callback,
                            language,
                            uploadedLocationCount
                    ),
                    PHOTO_UPLOAD_RETRY_DELAY_MS
            );
            return;
        }

        db.locationDao().markPhotoUploadFailed(location.getId(), error);
        summary.failedCount++;
        uploadNextPhoto(appContext, db, token, pendingPhotos, index + 1, 1, summary, mainHandler, callback, language, uploadedLocationCount);
    }

    private static class PhotoUploadSummary {
        int successCount;
        int failedCount;
    }
}
