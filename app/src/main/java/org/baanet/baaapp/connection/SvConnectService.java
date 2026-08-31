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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SvConnectService {
    private static final String TAG = "BaaSync";
    private static final String PREF = "baa_prefs";
    private static final String KEY_TOKEN = "token";

    public interface UploadCallback {
        void onComplete(int uploadedCount);
        void onNoTarget();
        void onError(String message);
    }

    public static void upload(Context context) {
        LanguageService language = LanguageService.get(context);
        upload(context, new UploadCallback() {
            @Override
            public void onComplete(int uploadedCount) {
                Toast.makeText(
                        context,
                        language.format("sync.complete", uploadedCount),
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

        // uploadFlgが立っていないデータを一括所得
        String ownerPublicId = UserDataScope.getCurrentPublicId(appContext);
        Log.d(TAG, "Upload ownerPublicId=" + ownerPublicId);
        List<LocationEntity> unuploadedLocations = ownerPublicId != null
                ? db.locationDao().getUnuploadedLocationsByOwner(ownerPublicId)
                : db.locationDao().getUnownedUnuploadedLocations();
        Log.d(TAG, "Upload target count=" + unuploadedLocations.size());

        if (unuploadedLocations.isEmpty()) {
            Log.d(TAG, "Upload skipped: no target locations");
            callback.onNoTarget();
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
                    if (response == null || !response.isOk() || response.uploadedLocalIds == null) {
                        Log.w(TAG, "Upload failed: invalid success response");
                        callback.onError(language.format("sync.failed", ""));
                        return;
                    }

                    Log.d(TAG, "Upload succeeded uploadedLocalIds=" + response.uploadedLocalIds);
                    db.locationDao().markUploaded(response.uploadedLocalIds);
                    callback.onComplete(response.uploadedLocalIds.size());
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
}
