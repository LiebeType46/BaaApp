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

public class RegisterActivity extends AppCompatActivity {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // ★サーバーIPはあなたの環境に合わせる（例：192.168.1.6）
    private static final String BASE_URL = "http://192.168.1.6:8080";

    private static final String PREF = "baa_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_PUBLIC_ID = "public_id";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnRegister;
    private Button btnBackToLogin;
    private ProgressBar progress;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
        progress = findViewById(R.id.progress);
        tvResult = findViewById(R.id.tvResult);

        btnRegister.setOnClickListener(v -> doRegister());
        btnBackToLogin.setOnClickListener(v -> finish()); // Loginから起動してる想定なら戻るだけでOK
    }

    private void doRegister() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            tvResult.setText("Please fill all fields.");
            return;
        }

        setLoading(true);
        tvResult.setText("");

        try {
            JSONObject body = new JSONObject();
            // ★サーバーDTOに合わせる：username/email/password が正しい前提
            body.put("username", username);
            body.put("email", email);
            body.put("password", password);

            Request req = new Request.Builder()
                    .url(BASE_URL + "/auth/register")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            client.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        tvResult.setText("Network error: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String resBody = response.body() != null ? response.body().string() : "";

                    runOnUiThread(() -> {
                        setLoading(false);
                        tvResult.setText("HTTP " + response.code() + "\n" + resBody);

                        if (!response.isSuccessful()) {
                            // 400/401/500 などはここで終了（必要ならエラーメッセージ整形）
                            return;
                        }

                        try {
                            AuthResponse resObj = gson.fromJson(resBody, AuthResponse.class);

                            // 必須チェック（サーバー実装がまだ揺れてる時期に効く）
                            if (resObj == null || resObj.token == null || resObj.token.isEmpty()
                                    || resObj.publicId == null || resObj.publicId.isEmpty()) {
                                tvResult.setText("Login OK but invalid response body.\n" + resBody);
                                return;
                            }

                            // オンライン用トークン保存（暫定）
                            getSharedPreferences(PREF, MODE_PRIVATE)
                                    .edit()
                                    .putString(KEY_TOKEN, resObj.token)
                                    .putString(KEY_PUBLIC_ID, resObj.publicId)
                                    .apply();

                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();

                        } catch (Exception e) {
                            // JSON形式が想定と違う・フィールド名違い・空文字など
                            tvResult.setText("Parse error: " + e.getMessage() + "\n" + resBody);
                        }
                    });
                }

            });

        } catch (Exception e) {
            setLoading(false);
            tvResult.setText("Error: " + e.getMessage());
        }
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        btnBackToLogin.setEnabled(!loading);
    }
}
