package com.example.baapp.api;

import android.content.Context;

import com.example.baapp.common.LanguageService;
import com.example.baapp.login.AuthResponse;
import com.example.baapp.login.LoginRequest;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthApi {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface AuthResultCallback {
        void onSuccess(AuthResponse response);
        void onError(String message);
    }
    private static final Gson gson = new Gson();
    private static final OkHttpClient client = ApiClient.getClient();

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

    public static void register(Context context, String username, String email, String password, AuthResultCallback callback) {
        LanguageService language = LanguageService.get(context);
        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("email", email);
            body.put("password", password);

            Request req = new Request.Builder()
                    .url(ApiConfig.BASE_URL + ApiEndpoint.REGISTER)
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            client.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(language.format("register.network_error", e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String resBody = response.body() != null ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        callback.onError(language.format("register.http_error", response.code(), resBody));
                        return;
                    }

                    try {
                        AuthResponse resObj = gson.fromJson(resBody, AuthResponse.class);

                        if (resObj == null || resObj.token == null || resObj.token.isEmpty()
                                || resObj.publicId == null || resObj.publicId.isEmpty()) {
                            callback.onError(language.format("register.invalid_response", resBody));
                            return;
                        }

                        callback.onSuccess(resObj);

                    } catch (Exception e) {
                        callback.onError(language.format("register.parse_error", e.getMessage(), resBody));
                    }
                }
            });

        } catch (Exception e) {
            callback.onError(language.format("register.error", e.getMessage()));
        }
    }
}
