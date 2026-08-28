package org.baanet.baaapp.common;

import android.content.Context;
import android.content.SharedPreferences;

import org.baanet.baaapp.data.AppDatabase;

public final class UserDataScope {

    private static final String PREF = "baa_prefs";
    private static final String KEY_PUBLIC_ID = "public_id";

    private UserDataScope() {
    }

    public static String getCurrentPublicId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String publicId = prefs.getString(KEY_PUBLIC_ID, null);
        if (publicId == null) {
            return null;
        }

        String trimmed = publicId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static void claimUnownedLocalData(Context context, String publicId) {
        String normalizedPublicId = normalizePublicId(publicId);
        if (normalizedPublicId == null) {
            return;
        }

        AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
        db.locationDao().claimUnowned(normalizedPublicId);
        db.searchConditionDao().claimUnowned(normalizedPublicId);
    }

    public static String getSearchConditionId(String publicId, String kind) {
        String normalizedPublicId = normalizePublicId(publicId);
        if (normalizedPublicId == null) {
            return kind;
        }
        return normalizedPublicId + ":" + kind;
    }

    private static String normalizePublicId(String publicId) {
        if (publicId == null) {
            return null;
        }

        String trimmed = publicId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
