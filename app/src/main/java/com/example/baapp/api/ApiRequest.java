package com.example.baapp.api;

import okhttp3.Request;

public class ApiRequest {

    public static Request authGet(String path, String token) {
        return new Request.Builder()
                .url(ApiConfig.BASE_URL + path)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

}