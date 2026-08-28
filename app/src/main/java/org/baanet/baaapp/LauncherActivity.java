package org.baanet.baaapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.baanet.baaapp.api.ApiEndpoint;
import org.baanet.baaapp.api.ApiRequest;
import org.baanet.baaapp.api.AuthApi;
import org.baanet.baaapp.common.UserDataScope;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class LauncherActivity extends AppCompatActivity {

    private static final String PREF = "baa_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_PUBLIC_ID = "public_id";

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
                    String resBody = response.body() != null ? response.body().string() : "";
                    String publicId = readPublicId(resBody);
                    if (publicId != null) {
                        getSharedPreferences(PREF, MODE_PRIVATE)
                                .edit()
                                .putString(KEY_PUBLIC_ID, publicId)
                                .apply();
                        UserDataScope.claimUnownedLocalData(LauncherActivity.this, publicId);
                    }
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
        startActivity(new Intent(this, org.baanet.baaapp.login.LoginActivity.class));
        finish();
    }

    private void goMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private String readPublicId(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            String publicId = json.optString(KEY_PUBLIC_ID, null);
            if (publicId == null || publicId.isBlank()) {
                return null;
            }
            return publicId;
        } catch (Exception e) {
            return null;
        }
    }
}
