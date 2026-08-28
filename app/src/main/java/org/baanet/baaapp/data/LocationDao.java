package org.baanet.baaapp.data;

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

    @Query("SELECT * FROM locations WHERE ownerPublicId = :ownerPublicId ORDER BY timestamp DESC")
    List<LocationEntity> getAllLocationsLatestFirstByOwner(String ownerPublicId);

    @Query("SELECT * FROM locations WHERE ownerPublicId IS NULL ORDER BY timestamp DESC")
    List<LocationEntity> getAllUnownedLocationsLatestFirst();

    @Query("DELETE FROM locations WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM locations")
    void deleteAll();

    @Query("SELECT * FROM locations WHERE ownerPublicId = :ownerPublicId AND timestamp >= :threshold")
    List<LocationEntity> getLocationsWithinTimeRangeByOwner(String ownerPublicId, String threshold);

    @Query("SELECT * FROM locations WHERE ownerPublicId IS NULL AND timestamp >= :threshold")
    List<LocationEntity> getUnownedLocationsWithinTimeRange(String threshold);

    @Query("SELECT * FROM locations WHERE ownerPublicId = :ownerPublicId AND uploadFlg = 0 ORDER BY id ASC")
    List<LocationEntity> getUnuploadedLocationsByOwner(String ownerPublicId);

    @Query("SELECT * FROM locations WHERE ownerPublicId IS NULL AND uploadFlg = 0 ORDER BY id ASC")
    List<LocationEntity> getUnownedUnuploadedLocations();

    @Query("UPDATE locations SET uploadFlg = 1 WHERE id IN (:ids)")
    int markUploaded(List<Long> ids);

    @Query("SELECT * FROM locations WHERE ownerPublicId = :ownerPublicId ORDER BY timestamp DESC LIMIT :limit")
    List<LocationEntity> getLatestLocationsByOwner(String ownerPublicId, int limit);

    @Query("SELECT * FROM locations WHERE ownerPublicId IS NULL ORDER BY timestamp DESC LIMIT :limit")
    List<LocationEntity> getLatestUnownedLocations(int limit);

    @Query("SELECT * FROM locations WHERE id = :id AND ownerPublicId = :ownerPublicId LIMIT 1")
    LocationEntity findByIdAndOwner(int id, String ownerPublicId);

    @Query("SELECT * FROM locations WHERE id = :id AND ownerPublicId IS NULL LIMIT 1")
    LocationEntity findUnownedById(int id);

    @Query("UPDATE locations SET ownerPublicId = :ownerPublicId WHERE ownerPublicId IS NULL")
    int claimUnowned(String ownerPublicId);
}
