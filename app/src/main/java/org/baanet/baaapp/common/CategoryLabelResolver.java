package org.baanet.baaapp.common;

import android.content.Context;

import org.baanet.baaapp.R;

public class CategoryLabelResolver {

    public static int getLabelRes(MainCategory category) {
        if (category == null) return R.string.cat_daily;

        switch (category) {
            case EMERGENCY:
                return R.string.cat_emergency;
            case WARNING:
                return R.string.cat_warning;
            case INFO:
                return R.string.cat_info;
            case SUPPORT:
                return R.string.cat_support;
            case DAILY:
            default:
                return R.string.cat_daily;
        }
    }

    public static String getLabel(Context context, MainCategory category) {
        LanguageService language = LanguageService.get(context);
        return language.t(getLabelKey(category));
    }

    public static MainCategory fromLabel(Context context, String label) {
        if (label == null) {
            return null;
        }

        String normalizedLabel = label.trim();
        for (MainCategory category : MainCategory.values()) {
            if (getLabel(context, category).equals(normalizedLabel)) {
                return category;
            }
        }
        return null;
    }

    private static String getLabelKey(MainCategory category) {
        if (category == null) {
            return "category.daily";
        }

        switch (category) {
            case EMERGENCY:
                return "category.emergency";
            case WARNING:
                return "category.warning";
            case INFO:
                return "category.info";
            case SUPPORT:
                return "category.support";
            case DAILY:
            default:
                return "category.daily";
        }
    }
}
