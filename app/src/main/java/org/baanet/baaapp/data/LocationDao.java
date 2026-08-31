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

    @Query("SELECT * FROM locations WHERE ownerPublicId = :ownerPublicId AND (uploadFlg = 0 OR (serverLocationId IS NULL AND photoUri LIKE 'photos/%')) ORDER BY id ASC")
    List<LocationEntity> getLocationsNeedingServerSyncByOwner(String ownerPublicId);

    @Query("SELECT * FROM locations WHERE ownerPublicId IS NULL AND (uploadFlg = 0 OR (serverLocationId IS NULL AND photoUri LIKE 'photos/%')) ORDER BY id ASC")
    List<LocationEntity> getUnownedLocationsNeedingServerSync();

    @Query("UPDATE locations SET uploadFlg = 1 WHERE id IN (:ids)")
    int markUploaded(List<Integer> ids);

    @Query("UPDATE locations SET uploadFlg = 1, serverLocationId = :serverLocationId WHERE id = :localId")
    int markLocationUploaded(int localId, long serverLocationId);

    @Query("UPDATE locations SET photoUploadFlg = 1, photoUploadRetryCount = 0, lastPhotoUploadError = NULL WHERE id = :localId")
    int markPhotoUploaded(int localId);

    @Query("UPDATE locations SET photoUploadFlg = 0, photoUploadRetryCount = photoUploadRetryCount + 1, lastPhotoUploadError = :error WHERE id = :localId")
    int markPhotoUploadFailed(int localId, String error);

    @Query("SELECT * FROM locations WHERE ownerPublicId = :ownerPublicId AND uploadFlg = 1 AND photoUploadFlg = 0 AND serverLocationId IS NOT NULL AND photoUri LIKE 'photos/%' ORDER BY id ASC")
    List<LocationEntity> getPendingPhotoUploadsByOwner(String ownerPublicId);

    @Query("SELECT * FROM locations WHERE ownerPublicId IS NULL AND uploadFlg = 1 AND photoUploadFlg = 0 AND serverLocationId IS NOT NULL AND photoUri LIKE 'photos/%' ORDER BY id ASC")
    List<LocationEntity> getUnownedPendingPhotoUploads();

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
