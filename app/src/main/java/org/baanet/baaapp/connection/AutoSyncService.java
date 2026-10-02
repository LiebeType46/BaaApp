package org.baanet.baaapp.connection;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public final class AutoSyncService {

    private static final String TAG = "BaaSync";
    private static final String PREF = "baa_prefs";
    private static final String KEY_INTERVAL = "auto_sync_interval_minutes";
    private static final String KEY_LAST_SYNC_AT = "last_sync_at";
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static Context foregroundContext;
    private static Runnable scheduledSync;
    private static boolean syncing = false;

    private AutoSyncService() {
    }

    public static int[] getIntervalOptions() {
        return SyncSchedule.getIntervalOptions();
    }

    public static int getIntervalMinutes(Context context) {
        SharedPreferences prefs = prefs(context);
        int minutes = prefs.getInt(KEY_INTERVAL,
                prefs.getBoolean("auto_sync_enabled", true) ? SyncSchedule.DEFAULT_INTERVAL_MINUTES : 0);
        return SyncSchedule.isSupportedInterval(minutes) ? minutes : SyncSchedule.DEFAULT_INTERVAL_MINUTES;
    }

    public static synchronized void setIntervalMinutes(Context context, int minutes) {
        if (!SyncSchedule.isSupportedInterval(minutes)) {
            throw new IllegalArgumentException("Unsupported sync interval");
        }
        prefs(context).edit().putInt(KEY_INTERVAL, minutes).apply();
        Log.d(TAG, "Auto sync interval changed minutes=" + minutes);
        scheduleNextSync();
    }

    public static long getLastSyncAt(Context context) {
        SharedPreferences prefs = prefs(context);
        return prefs.getLong(KEY_LAST_SYNC_AT, prefs.getLong("last_auto_sync_at", 0L));
    }

    static synchronized void recordSyncStarted(Context context) {
        long now = System.currentTimeMillis();
        prefs(context).edit().putLong(KEY_LAST_SYNC_AT, now).apply();
        Log.d(TAG, "Sync start time updated lastSyncAt=" + now);
        scheduleNextSync();
    }

    public static synchronized void requestStartupSync(Context context) {
        foregroundContext = context.getApplicationContext();
        requestSync(foregroundContext, "startup");
    }

    public static synchronized void requestResumeSync(Context context) {
        foregroundContext = context.getApplicationContext();
        requestSync(foregroundContext, "resume");
    }

    public static void requestPostSync(Context context) {
        requestSync(context.getApplicationContext(), "post");
    }

    public static synchronized void stopForegroundSync() {
        foregroundContext = null;
        cancelScheduledSync();
    }

    private static void cancelScheduledSync() {
        if (scheduledSync != null) {
            handler.removeCallbacks(scheduledSync);
            scheduledSync = null;
        }
    }

    private static synchronized void scheduleNextSync() {
        cancelScheduledSync();
        if (foregroundContext == null || syncing) return;
        Context context = foregroundContext;
        int minutes = getIntervalMinutes(context);
        String token = prefs(context).getString("token", null);
        if (minutes == 0 || token == null || token.isBlank()) return;
        long delay = SyncSchedule.remainingDelay(System.currentTimeMillis(), getLastSyncAt(context), minutes);
        Log.d(TAG, "Auto sync scheduled delayMs=" + delay + ", intervalMinutes=" + minutes);
        scheduledSync = () -> {
            synchronized (AutoSyncService.class) {
                scheduledSync = null;
                if (foregroundContext == null) return;
                long remaining = SyncSchedule.remainingDelay(System.currentTimeMillis(),
                        getLastSyncAt(context), getIntervalMinutes(context));
                if (remaining > 0) {
                    scheduleNextSync();
                    return;
                }
                requestSync(context, "interval");
            }
        };
        handler.postDelayed(scheduledSync, delay);
    }

    private static synchronized void requestSync(Context context, String reason) {
        Context appContext = context.getApplicationContext();
        if (getIntervalMinutes(appContext) == 0) {
            Log.d(TAG, "Auto sync skipped reason=" + reason + ", autoSyncEnabled=false");
            return;
        }
        if (syncing) {
            Log.d(TAG, "Auto sync skipped reason=" + reason + ", already syncing");
            return;
        }

        String token = prefs(appContext).getString("token", null);
        if (token == null || token.isBlank()) {
            Log.d(TAG, "Auto sync skipped reason=" + reason + ", no login");
            return;
        }

        syncing = true;
        cancelScheduledSync();
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
        scheduleNextSync();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
}
