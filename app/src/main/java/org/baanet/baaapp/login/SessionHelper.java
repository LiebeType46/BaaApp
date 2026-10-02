package org.baanet.baaapp.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.baanet.baaapp.api.ApiClient;

public final class SessionHelper {
    private static final String PREF = "baa_prefs";

    private SessionHelper() {
    }

    public static void logout(Activity activity) {
        activity.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .remove("token")
                .remove("public_id")
                .remove("last_auto_sync_at")
                .apply();
        // Started syncs retain their captured credentials and use a separate dispatcher.
        ApiClient.getClient().dispatcher().cancelAll();

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }

    public static boolean isCurrentToken(Context context, String token) {
        return token != null && token.equals(context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString("token", null));
    }
}
