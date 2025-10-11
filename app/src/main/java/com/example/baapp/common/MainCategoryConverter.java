package com.example.baapp.common;

import androidx.room.TypeConverter;

public class MainCategoryConverter {

    @TypeConverter
    public static String fromCategory(MainCategory category) {
        return category == null ? null : category.name();
    }

    @TypeConverter
    public static MainCategory toCategory(String value) {
        if (value == null) return null;
        try {
            return MainCategory.valueOf(value);
        } catch (IllegalArgumentException e) {
            // 不正な文字列が保存されていた場合、デフォルト値などを返すか null にする
            return null;
        }
    }
}
