package com.example.baapp.search;

public class SearchCondition {

    private String category;
    private String subCategory;
    private String fromTimestamp;
    private String toTimestamp;
    private String memoKeyword;
    private Boolean hasPhoto;
    private Boolean uploadFlg;
    private Double radiusMeters;

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
        setCategory(category);
        setSubCategory(subCategory);
        setFromTimestamp(fromTimestamp);
        setToTimestamp(toTimestamp);
        setMemoKeyword(memoKeyword);
        this.hasPhoto = hasPhoto;
        this.uploadFlg = uploadFlg;
        this.radiusMeters = radiusMeters;
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

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
