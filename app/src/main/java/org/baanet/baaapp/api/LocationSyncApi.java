package org.baanet.baaapp.api;

import com.google.gson.Gson;

import org.baanet.baaapp.sync.LocationSyncRequest;
import org.baanet.baaapp.sync.LocationSyncResponse;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LocationSyncApi {

    public interface LocationSyncCallback {
        void onSuccess(LocationSyncResponse response);
        void onError(int httpCode, String responseBody);
        void onFailure(IOException e);
    }

    private static final Gson gson = new Gson();

    private LocationSyncApi() {
    }

    public static void sync(String token, LocationSyncRequest syncRequest, LocationSyncCallback callback) {
        String json = gson.toJson(syncRequest);
        ApiClient.getClient()
                .newCall(ApiRequest.authPost(ApiEndpoint.LOCATIONS_SYNC, token, json))
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        callback.onFailure(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            callback.onError(response.code(), responseBody);
                            return;
                        }

                        LocationSyncResponse syncResponse = gson.fromJson(responseBody, LocationSyncResponse.class);
                        callback.onSuccess(syncResponse);
                    }
                });
    }
}
