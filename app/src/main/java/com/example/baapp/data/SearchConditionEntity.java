package com.example.baapp.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "search_condition")
public class SearchConditionEntity {

    public static final String CURRENT_ID = "current";
    public static final String DEFAULT_ID = "default";

    @PrimaryKey
    @NonNull
    public String id = CURRENT_ID;

    public String category;
    public String subCategory;
    public String fromTimestamp;
    public String toTimestamp;
    public String memoKeyword;
    public Boolean hasPhoto;
    public Boolean uploadFlg;
    public Double radiusMeters;
    public Integer resultLimit;

    public SearchConditionEntity() {
    }
}
