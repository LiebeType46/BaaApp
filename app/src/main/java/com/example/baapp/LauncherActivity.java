package com.example.baapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.baapp.api.ApiEndpoint;
import com.example.baapp.api.ApiRequest;
import com.example.baapp.api.AuthApi;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class LauncherActivity extends AppCompatActivity {

    private static final String PREF = "baa_prefs";
    private static final String KEY_TOKEN = "token";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String token = getSharedPreferences(PREF, MODE_PRIVATE).getString(KEY_TOKEN, null);
        if (token == null || token.isBlank()) {
            goLogin();
            return;
        }

        // token有効性確認（/auth/me が無いなら /health でもOK）
        Request request = ApiRequest.authGet(ApiEndpoint.AUTH_ME, token);

        AuthApi.checkToken(token, new Callback() {

            @Override
            public void onFailure(@NonNull Call call, IOException e) {
                runOnUiThread(() -> goLogin());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                if (response.isSuccessful()) {
                    runOnUiThread(() -> goMain());
                } else {
                    getSharedPreferences(PREF, MODE_PRIVATE)
                            .edit()
                            .remove(KEY_TOKEN)
                            .apply();

                    runOnUiThread(() -> goLogin());
                }
            }
        });
    }

    private void goLogin() {
        startActivity(new Intent(this, com.example.baapp.login.LoginActivity.class));
        finish();
    }

    private void goMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
