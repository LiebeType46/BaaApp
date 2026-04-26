package com.example.baapp.login; // ←あなたのパッケージに合わせて変更

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.baapp.MainActivity;
import com.example.baapp.R;
import com.example.baapp.api.AuthApi;
import com.example.baapp.common.LanguageService;

public class RegisterActivity extends AppCompatActivity {

    private static final String PREF = "baa_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_PUBLIC_ID = "public_id";

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnRegister;
    private Button btnBackToLogin;
    private ProgressBar progress;
    private TextView tvResult;
    private TextView tvRegisterTitle;
    private LanguageService language;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        language = LanguageService.get(this);

        tvRegisterTitle = findViewById(R.id.tvRegisterTitle);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
        progress = findViewById(R.id.progress);
        tvResult = findViewById(R.id.tvResult);
        applyLanguage();

        btnRegister.setOnClickListener(v -> doRegister());
        btnBackToLogin.setOnClickListener(v -> finish()); // Loginから起動してる想定なら戻るだけでOK
    }

    private void doRegister() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            tvResult.setText(language.t("register.fill_all"));
            return;
        }

        setLoading(true);
        tvResult.setText("");

        AuthApi.register(this, username, email, password, new AuthApi.AuthResultCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                runOnUiThread(() -> {
                    setLoading(false);

                    getSharedPreferences(PREF, MODE_PRIVATE)
                            .edit()
                            .putString(KEY_TOKEN, response.token)
                            .putString(KEY_PUBLIC_ID, response.publicId)
                            .apply();

                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    setLoading(false);
                    tvResult.setText(message);
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        btnBackToLogin.setEnabled(!loading);
    }

    private void applyLanguage() {
        tvRegisterTitle.setText(language.t("register.title"));
        etUsername.setHint(language.t("register.username_hint"));
        etEmail.setHint(language.t("register.email_hint"));
        etPassword.setHint(language.t("register.password_hint"));
        btnRegister.setText(language.t("register.button"));
        btnBackToLogin.setText(language.t("register.back_to_login"));
    }
}
