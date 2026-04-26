package com.example.baapp.search;

import com.example.baapp.data.SearchConditionEntity;

public class SearchConditionMapper {

    private SearchConditionMapper() {
    }

    public static SearchConditionEntity toEntity(SearchCondition condition) {
        SearchCondition safeCondition = condition != null ? condition : new SearchCondition();

        SearchConditionEntity entity = new SearchConditionEntity();
        entity.id = SearchConditionEntity.CURRENT_ID;
        entity.category = safeCondition.getCategory();
        entity.subCategory = safeCondition.getSubCategory();
        entity.fromTimestamp = safeCondition.getFromTimestamp();
        entity.toTimestamp = safeCondition.getToTimestamp();
        entity.memoKeyword = safeCondition.getMemoKeyword();
        entity.hasPhoto = safeCondition.getHasPhoto();
        entity.uploadFlg = safeCondition.getUploadFlg();
        entity.radiusMeters = safeCondition.getRadiusMeters();

        return entity;
    }

    public static SearchCondition toDto(SearchConditionEntity entity) {
        if (entity == null) {
            return new SearchCondition();
        }

        return new SearchCondition(
                entity.category,
                entity.subCategory,
                entity.fromTimestamp,
                entity.toTimestamp,
                entity.memoKeyword,
                entity.hasPhoto,
                entity.uploadFlg,
                entity.radiusMeters
        );
    }
}
