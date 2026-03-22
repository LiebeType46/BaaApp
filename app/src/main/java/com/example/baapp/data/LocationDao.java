package com.example.baapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LocationEntity location);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LocationEntity> locationList);

    @Query("SELECT * FROM locations ORDER BY timestamp ASC")
    List<LocationEntity> getAllLocations();

    @Query("DELETE FROM locations WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM locations")
    void deleteAll();

    @Query("SELECT * FROM locations WHERE timestamp BETWEEN :start AND :end")
    List<LocationEntity> getLocationsBetween(String start, String end);

    @Query("SELECT * FROM locations WHERE memo LIKE '%' || :keyword || '%'")
    List<LocationEntity> searchByMemo(String keyword);

    @Query("SELECT * FROM locations WHERE timestamp >= :threshold")
    List<LocationEntity> getLocationsWithinTimeRange(String threshold);

    @Query("SELECT * FROM locations WHERE uploadFlg = 0 ORDER BY id ASC")
    List<LocationEntity> getUnuploadedLocations();

    @Query("SELECT COUNT(*) FROM locations WHERE uploadFlg = 0")
    int countUnuploaded();

    @Query("UPDATE locations SET uploadFlg = 1 WHERE id IN (:ids)")
    int markUploaded(List<Long> ids);

    @Query("SELECT * FROM locations ORDER BY timestamp DESC LIMIT :limit")
    List<LocationEntity> getLatestLocations(int limit);

    @Query("SELECT * FROM locations WHERE id = :id LIMIT 1")
    LocationEntity findById(int id);
}