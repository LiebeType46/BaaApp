package org.baanet.baaapp.api;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class ApiRequest {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public static Request authGet(String path, String token) {
        return new Request.Builder()
                .url(ApiConfig.BASE_URL + path)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

    public static Request authPost(String path, String token, String json) {
        return new Request.Builder()
                .url(ApiConfig.BASE_URL + path)
                .post(RequestBody.create(json, JSON))
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

}
