package com.example.baapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface SearchConditionDao {

    @Query("SELECT * FROM search_condition WHERE id = 'current' LIMIT 1")
    SearchConditionEntity getCurrent();

    @Query("SELECT * FROM search_condition WHERE id = 'default' LIMIT 1")
    SearchConditionEntity getDefault();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(SearchConditionEntity entity);

    @Query("DELETE FROM search_condition WHERE id = 'current'")
    void clearCurrent();
}
