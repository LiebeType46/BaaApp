package com.example.baapp.search;

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
        this.category = normalize(category);
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

    private static Integer normalizeLimit(Integer value) {
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }
}
