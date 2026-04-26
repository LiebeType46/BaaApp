package com.example.baapp.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.baapp.MainActivity;
import com.example.baapp.R;
import com.example.baapp.api.AuthApi;
import com.example.baapp.common.LanguageService;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String PREF = "baa_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_PUBLIC_ID = "public_id";
    private final Gson gson = new Gson();

    private EditText etIdentifier;
    private EditText etPassword;
    private TextView tvStatus;
    private LanguageService language;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        language = LanguageService.get(this);

        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword   = findViewById(R.id.etPassword);
        tvStatus     = findViewById(R.id.tvStatus);
        applyLanguage();

        findViewById(R.id.btnLogin).setOnClickListener(v -> doLogin());
        findViewById(R.id.btnGoRegister).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void doLogin() {

        String identifier = etIdentifier.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (identifier.isEmpty() || password.isEmpty()) {
            tvStatus.setText(language.t("login.input_required"));
            return;
        }

        LoginRequest req = new LoginRequest();
        req.usernameOrEmail = identifier;
        req.password = password;

        AuthApi.login(req, new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> tvStatus.setText(language.t("login.network_error")));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                String resBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> tvStatus.setText(language.format("login.failed", response.code())));
                    return;
                }

                AuthResponse resObj = new Gson().fromJson(resBody, AuthResponse.class);

                getSharedPreferences(PREF, MODE_PRIVATE)
                        .edit()
                        .putString(KEY_TOKEN, resObj.token)
                        .putString(KEY_PUBLIC_ID, resObj.publicId)
                        .apply();

                runOnUiThread(() -> {
                    tvStatus.setText(language.t("login.ok"));
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                });
            }
        });
    }

    private void applyLanguage() {
        etIdentifier.setHint(language.t("login.identifier_hint"));
        etPassword.setHint(language.t("login.password_hint"));
        ((TextView) findViewById(R.id.btnLogin)).setText(language.t("login.login"));
        ((TextView) findViewById(R.id.btnGoRegister)).setText(language.t("login.register"));
    }
}
