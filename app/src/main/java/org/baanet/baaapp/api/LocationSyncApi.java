package org.baanet.baaapp.api;

import android.util.Log;

import com.google.gson.Gson;

import org.baanet.baaapp.sync.LocationSyncRequest;
import org.baanet.baaapp.sync.LocationSyncResponse;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LocationSyncApi {

    private static final String TAG = "BaaSync";

    public interface LocationSyncCallback {
        void onSuccess(LocationSyncResponse response);
        void onError(int httpCode, String responseBody);
        void onFailure(IOException e);
    }

    public interface PhotoUploadCallback {
        void onSuccess(String responseBody);
        void onError(int httpCode, String responseBody);
        void onFailure(IOException e);
    }

    private static final Gson gson = new Gson();

    private LocationSyncApi() {
    }

    public static void sync(String token, LocationSyncRequest syncRequest, LocationSyncCallback callback) {
        String json = gson.toJson(syncRequest);
        Request request = ApiRequest.authPost(ApiEndpoint.LOCATIONS_SYNC, token, json);
        Log.d(TAG, "Location sync request url=" + request.url());
        Log.d(TAG, "Location sync request method=" + request.method());
        Log.d(TAG, "Location sync request body=" + json);

        ApiClient.getClient()
                .newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Location sync network failure url=" + call.request().url(), e);
                        callback.onFailure(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        Log.d(TAG, "Location sync response url=" + response.request().url());
                        Log.d(TAG, "Location sync response code=" + response.code());
                        Log.d(TAG, "Location sync response message=" + response.message());
                        Log.d(TAG, "Location sync response body=" + responseBody);

                        if (!response.isSuccessful()) {
                            callback.onError(response.code(), responseBody);
                            return;
                        }

                        try {
                            LocationSyncResponse syncResponse = gson.fromJson(responseBody, LocationSyncResponse.class);
                            callback.onSuccess(syncResponse);
                        } catch (Exception e) {
                            Log.e(TAG, "Location sync response parse failure body=" + responseBody, e);
                            callback.onError(response.code(), responseBody);
                        }
                    }
                });
    }

    public static void uploadPhoto(
            String token,
            long serverLocationId,
            File photoFile,
            PhotoUploadCallback callback
    ) {
        RequestBody photoBody = RequestBody.create(photoFile, MediaType.get("image/jpeg"));
        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("photo", photoFile.getName(), photoBody)
                .build();

        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + ApiEndpoint.locationPhoto(serverLocationId))
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        Log.d(TAG, "Photo upload request url=" + request.url());
        Log.d(TAG, "Photo upload file=" + photoFile.getAbsolutePath()
                + ", exists=" + photoFile.exists()
                + ", length=" + photoFile.length());

        ApiClient.getClient()
                .newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Photo upload network failure url=" + call.request().url(), e);
                        callback.onFailure(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        Log.d(TAG, "Photo upload response url=" + response.request().url());
                        Log.d(TAG, "Photo upload response code=" + response.code());
                        Log.d(TAG, "Photo upload response message=" + response.message());
                        Log.d(TAG, "Photo upload response body=" + responseBody);

                        if (!response.isSuccessful()) {
                            callback.onError(response.code(), responseBody);
                            return;
                        }

                        callback.onSuccess(responseBody);
                    }
                });
    }
}
