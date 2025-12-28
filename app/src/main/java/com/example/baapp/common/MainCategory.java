package com.example.baapp.common;

import com.example.baapp.R;

public enum MainCategory {
    EMERGENCY("EMERGENCY", R.string.cat_emergency),
    WARNING("WARNING", R.string.cat_warning),
    INFO("INFO", R.string.cat_info),
    SUPPORT("SUPPORT", R.string.cat_support),
    DAILY("DAILY", R.string.cat_daily);

    private final String id;
    private final int label;

    MainCategory(String id, int label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public int getLabel() {
        return label;
    }

    public static MainCategory fromId(String id) {
        for (MainCategory category : values()) {
            if (category.getId().equals(id)) {
                return category;
            }
        }
        return DAILY;
    }

    public static MainCategory fromLabel(int label) {
        for (MainCategory category : values()) {
            if (category.getLabel() == label) {
                return category;
            }
        }
        return DAILY; // デフォルト（必要に応じて変更可）
    }
}
