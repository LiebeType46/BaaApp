package com.example.baapp.api;

import com.example.baapp.login.LoginRequest;
import com.google.gson.Gson;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class AuthApi {

    private static final Gson gson = new Gson();

    public static void login(LoginRequest req, Callback callback) {

        String json = gson.toJson(req);

        RequestBody body = RequestBody.create(
                json,
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + ApiEndpoint.LOGIN)
                .post(body)
                .build();

        Call call = ApiClient.getClient().newCall(request);
        call.enqueue(callback);
    }

    public static void checkToken(String token, Callback callback) {

        Call call = ApiClient.getClient().newCall(
                ApiRequest.authGet(ApiEndpoint.AUTH_ME, token)
        );

        call.enqueue(callback);
    }
}