package com.example.baapp.data;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.baapp.common.MainCategoryConverter;

@Database(entities = {LocationEntity.class}, version = 4, exportSchema = false)
@TypeConverters({MainCategoryConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;
    private static final String DB_NAME = "locations.db";

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME
                            )
                            .setJournalMode(JournalMode.TRUNCATE)
                            .allowMainThreadQueries() // 必要に応じて非同期へ移行
                            .addMigrations(MIGRATION_2_3)
                            .addMigrations(MIGRATION_3_4)
                            .build();
                    Log.d("AppDatabase", "AppDatabase initialized");
                }
            }
        }
        return instance;
    }

    public void checkpoint() {
        if (instance != null) {
            instance.getOpenHelper().getWritableDatabase().execSQL("PRAGMA wal_checkpoint(FULL)");
            instance.close();
            Log.d("AppDatabase", "チェックポイント & DB クローズ完了");
        }
    }

    public void flushToDisk() {
        try {
            instance.getOpenHelper().getWritableDatabase().execSQL("PRAGMA wal_checkpoint(FULL)");
            Log.d("AppDatabase", "WAL チェックポイントを実行しました。");
        } catch (Exception e) {
            Log.e("AppDatabase", "チェックポイント処理に失敗しました。", e);
        }
    }

    public abstract LocationDao locationDao();

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE locations ADD COLUMN category TEXT DEFAULT '日常'");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE locations ADD COLUMN subCategory TEXT");
        }
    };
}