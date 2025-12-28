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
        return context.getString(getLabelRes(category));
    }
}
