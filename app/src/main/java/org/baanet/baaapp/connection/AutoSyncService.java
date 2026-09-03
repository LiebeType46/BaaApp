package org.baanet.baaapp.connection;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public final class AutoSyncService {

    private static final String TAG = "BaaSync";
    private static final String PREF = "baa_prefs";
    private static final String KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled";
    private static final String KEY_LAST_AUTO_SYNC_AT = "last_auto_sync_at";
    private static final long LIFECYCLE_MIN_INTERVAL_MS = 5 * 60 * 1000L;
    private static final long STARTUP_DELAY_MS = 5_000L;

    private static boolean syncing = false;

    private AutoSyncService() {
    }

    public static boolean isAutoSyncEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_SYNC_ENABLED, true);
    }

    public static void setAutoSyncEnabled(Context context, boolean enabled) {
        prefs(context).edit()
                .putBoolean(KEY_AUTO_SYNC_ENABLED, enabled)
                .apply();
        Log.d(TAG, "Auto sync setting changed enabled=" + enabled);
    }

    public static void requestStartupSync(Context context) {
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> requestLifecycleSync(context, "startup"),
                STARTUP_DELAY_MS
        );
    }

    public static void requestResumeSync(Context context) {
        requestLifecycleSync(context, "resume");
    }

    public static void requestPostSync(Context context) {
        requestSync(context, "post", false);
    }

    private static void requestLifecycleSync(Context context, String reason) {
        requestSync(context, reason, true);
    }

    private static synchronized void requestSync(Context context, String reason, boolean throttleByTime) {
        Context appContext = context.getApplicationContext();
        if (!isAutoSyncEnabled(appContext)) {
            Log.d(TAG, "Auto sync skipped reason=" + reason + ", autoSyncEnabled=false");
            return;
        }
        if (syncing) {
            Log.d(TAG, "Auto sync skipped reason=" + reason + ", already syncing");
            return;
        }

        long now = System.currentTimeMillis();
        long lastSyncAt = prefs(appContext).getLong(KEY_LAST_AUTO_SYNC_AT, 0L);
        if (throttleByTime && now - lastSyncAt < LIFECYCLE_MIN_INTERVAL_MS) {
            Log.d(TAG, "Auto sync skipped reason=" + reason + ", lastSyncAgoMs=" + (now - lastSyncAt));
            return;
        }

        syncing = true;
        prefs(appContext).edit()
                .putLong(KEY_LAST_AUTO_SYNC_AT, now)
                .apply();
        Log.d(TAG, "Auto sync started reason=" + reason);

        SvConnectService.upload(appContext, new SvConnectService.UploadCallback() {
            @Override
            public void onComplete(int uploadedCount, int photoUploadedCount, int photoFailedCount) {
                finish(reason, "complete locations=" + uploadedCount
                        + ", photos=" + photoUploadedCount
                        + ", photoFailed=" + photoFailedCount);
            }

            @Override
            public void onNoTarget() {
                finish(reason, "no target");
            }

            @Override
            public void onError(String message) {
                finish(reason, "error=" + message);
            }
        });
    }

    private static synchronized void finish(String reason, String result) {
        syncing = false;
        Log.d(TAG, "Auto sync finished reason=" + reason + ", result=" + result);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
}
