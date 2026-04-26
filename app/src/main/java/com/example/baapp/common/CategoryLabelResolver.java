package com.example.baapp.common;

import android.content.Context;

import com.example.baapp.R;

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
        if (category == null) {
            return language.t("category.daily");
        }

        switch (category) {
            case EMERGENCY:
                return language.t("category.emergency");
            case WARNING:
                return language.t("category.warning");
            case INFO:
                return language.t("category.info");
            case SUPPORT:
                return language.t("category.support");
            case DAILY:
            default:
                return language.t("category.daily");
        }
    }
}
