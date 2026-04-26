package com.example.baapp.common;

import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.IdRes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

public class LanguageService {

    private static final String TAG = "LanguageService";
    private static final String DEFAULT_LANGUAGE_FILE = "language_jp.properties";

    private static volatile LanguageService instance;

    private final Properties strings = new Properties();

    private LanguageService(Context context) {
        load(context, DEFAULT_LANGUAGE_FILE);
    }

    public static LanguageService get(Context context) {
        if (instance == null) {
            synchronized (LanguageService.class) {
                if (instance == null) {
                    instance = new LanguageService(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public String t(String key) {
        return strings.getProperty(key, key);
    }

    public String format(String key, Object... args) {
        return String.format(Locale.getDefault(), t(key), args);
    }

    public void setText(View root, @IdRes int viewId, String key) {
        TextView view = root.findViewById(viewId);
        if (view != null) {
            view.setText(t(key));
        }
    }

    public void setHint(View root, @IdRes int viewId, String key) {
        TextView view = root.findViewById(viewId);
        if (view != null) {
            view.setHint(t(key));
        }
    }

    public void setMenuTitle(Menu menu, int itemId, String key) {
        if (menu == null) {
            return;
        }

        MenuItem item = menu.findItem(itemId);
        if (item != null) {
            item.setTitle(t(key));
        }
    }

    private void load(Context context, String fileName) {
        try (InputStream input = context.getAssets().open(fileName);
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            strings.clear();
            strings.load(reader);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load language file: " + fileName, e);
        }
    }
}
