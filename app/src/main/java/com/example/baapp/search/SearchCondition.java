package com.example.baapp.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchCondition {

    public static final int DEFAULT_RESULT_LIMIT = 50;

    private String category;
    private String subCategory;
    private String fromTimestamp;
    private String toTimestamp;
    private String memoKeyword;
    private Boolean hasPhoto;
    private Boolean uploadFlg;
    private Double radiusMeters;
    private Integer resultLimit;

    public SearchCondition() {
    }

    public SearchCondition(
            String category,
            String subCategory,
            String fromTimestamp,
            String toTimestamp,
            String memoKeyword,
            Boolean hasPhoto,
            Boolean uploadFlg,
            Double radiusMeters
    ) {
        this(
                category,
                subCategory,
                fromTimestamp,
                toTimestamp,
                memoKeyword,
                hasPhoto,
                uploadFlg,
                radiusMeters,
                null
        );
    }

    public SearchCondition(
            String category,
            String subCategory,
            String fromTimestamp,
            String toTimestamp,
            String memoKeyword,
            Boolean hasPhoto,
            Boolean uploadFlg,
            Double radiusMeters,
            Integer resultLimit
    ) {
        setCategory(category);
        setSubCategory(subCategory);
        setFromTimestamp(fromTimestamp);
        setToTimestamp(toTimestamp);
        setMemoKeyword(memoKeyword);
        this.hasPhoto = hasPhoto;
        this.uploadFlg = uploadFlg;
        this.radiusMeters = radiusMeters;
        this.resultLimit = normalizeLimit(resultLimit);
    }

    public boolean hasAnyCondition() {
        return category != null
                || subCategory != null
                || fromTimestamp != null
                || toTimestamp != null
                || memoKeyword != null
                || hasPhoto != null
                || uploadFlg != null
                || radiusMeters != null;
    }

    public boolean hasCustomResultLimit() {
        return resultLimit != null && resultLimit != DEFAULT_RESULT_LIMIT;
    }

    public static SearchCondition createDefault() {
        return new SearchCondition(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                DEFAULT_RESULT_LIMIT
        );
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = normalizeCategoryList(category);
    }

    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        if (category == null) {
            return categories;
        }

        for (String value : category.split(",")) {
            String normalized = normalize(value);
            if (normalized != null) {
                categories.add(normalized);
            }
        }

        return categories;
    }

    public void setCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            this.category = null;
            return;
        }

        List<String> normalizedCategories = new ArrayList<>();
        for (String value : categories) {
            String normalized = normalize(value);
            if (normalized != null) {
                normalizedCategories.add(normalized);
            }
        }

        this.category = normalizedCategories.isEmpty()
                ? null
                : String.join(",", normalizedCategories);
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = normalize(subCategory);
    }

    public String getFromTimestamp() {
        return fromTimestamp;
    }

    public void setFromTimestamp(String fromTimestamp) {
        this.fromTimestamp = normalize(fromTimestamp);
    }

    public String getToTimestamp() {
        return toTimestamp;
    }

    public void setToTimestamp(String toTimestamp) {
        this.toTimestamp = normalize(toTimestamp);
    }

    public String getMemoKeyword() {
        return memoKeyword;
    }

    public void setMemoKeyword(String memoKeyword) {
        this.memoKeyword = normalize(memoKeyword);
    }

    public Boolean getHasPhoto() {
        return hasPhoto;
    }

    public void setHasPhoto(Boolean hasPhoto) {
        this.hasPhoto = hasPhoto;
    }

    public Boolean getUploadFlg() {
        return uploadFlg;
    }

    public void setUploadFlg(Boolean uploadFlg) {
        this.uploadFlg = uploadFlg;
    }

    public Double getRadiusMeters() {
        return radiusMeters;
    }

    public void setRadiusMeters(Double radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    public Integer getResultLimit() {
        return resultLimit;
    }

    public void setResultLimit(Integer resultLimit) {
        this.resultLimit = normalizeLimit(resultLimit);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeCategoryList(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }

        List<String> values = Arrays.asList(normalized.split(","));
        List<String> normalizedValues = new ArrayList<>();
        for (String category : values) {
            String normalizedCategory = normalize(category);
            if (normalizedCategory != null) {
                normalizedValues.add(normalizedCategory);
            }
        }

        return normalizedValues.isEmpty() ? null : String.join(",", normalizedValues);
    }

    private static Integer normalizeLimit(Integer value) {
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }
}
