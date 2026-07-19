package org.baanet.baaapp.common;

import androidx.room.TypeConverter;

public class MainCategoryConverter {

    @TypeConverter
    public static String fromCategory(MainCategory category) {
        return category == null ? null : category.getId();
    }

    @TypeConverter
    public static MainCategory toCategory(String value) {
        if (value == null) {
            return null;
        }

        MainCategory category = MainCategory.fromIdOrNull(value);
        if (category != null) {
            return category;
        }

        switch (value.trim()) {
            case "緊急":
                return MainCategory.EMERGENCY;
            case "警告":
                return MainCategory.WARNING;
            case "情報":
                return MainCategory.INFO;
            case "支援":
                return MainCategory.SUPPORT;
            case "日常":
                return MainCategory.DAILY;
            default:
                return null;
        }
    }

    public static String normalizeCategoryId(String value) {
        MainCategory category = toCategory(value);
        return category == null ? null : category.getId();
    }

    public static String normalizeCategoryIdOrDefault(String value) {
        String categoryId = normalizeCategoryId(value);
        return categoryId == null ? MainCategory.DAILY.getId() : categoryId;
    }
}
