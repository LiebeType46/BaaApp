package org.baanet.baaapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface SearchConditionDao {

    @Query("SELECT * FROM search_condition WHERE id = :id LIMIT 1")
    SearchConditionEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(SearchConditionEntity entity);

    @Query("DELETE FROM search_condition WHERE id = :id")
    void clearById(String id);

    @Query("UPDATE search_condition SET ownerPublicId = :ownerPublicId, id = :ownerPublicId || ':' || id WHERE ownerPublicId IS NULL AND id IN ('current', 'default') AND NOT EXISTS (SELECT 1 FROM search_condition existing WHERE existing.id = :ownerPublicId || ':' || search_condition.id)")
    int claimUnowned(String ownerPublicId);
}
