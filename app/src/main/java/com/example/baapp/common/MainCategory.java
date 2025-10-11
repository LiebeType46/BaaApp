package com.example.baapp.common;

public enum MainCategory {
        EMERGENCY("緊急"),
        WARNING("警告"),
        INFO("情報"),
        SUPPORT("支援"),
        DAILY("日常");

        private final String label;

        MainCategory(String label) {
                this.label = label;
        }

        public String getLabel() {
                return label;
        }

        public static MainCategory fromLabel(String label) {
                for (MainCategory category : values()) {
                        if (category.getLabel().equals(label)) {
                                return category;
                        }
                }
                return DAILY; // デフォルト（必要に応じて変更可）
        }
}
