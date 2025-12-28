package com.example.baapp.data;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.baapp.common.MainCategoryConverter;

@Database(entities = {LocationEntity.class}, version = 6, exportSchema = false)
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
                            .addMigrations(MIGRATION_4_5)
                            .addMigrations(MIGRATION_5_6)
                            .build();
                    Log.d("AppDatabase", "AppDatabase initialized");
                }
            }
        }
        return instance;
    }

    public void checkpoint() {
        if (instance != null) {
            SupportSQLiteDatabase db =
                    instance.getOpenHelper().getWritableDatabase();

            Cursor cursor = db.query("PRAGMA wal_checkpoint(FULL)");
            cursor.close();

            instance.close();
            Log.d("AppDatabase", "チェックポイント & DB クローズ完了");
        }
    }

    public void flushToDisk() {
        try {
            SupportSQLiteDatabase db =
                    instance.getOpenHelper().getWritableDatabase();

            Cursor cursor = db.query("PRAGMA wal_checkpoint(FULL)");
            cursor.close();

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

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 日本語 → enum名 へ変換
            database.execSQL(
                    "UPDATE locations SET category = 'DAILY' WHERE category IS NULL OR category = '日常'"
            );
            database.execSQL(
                    "UPDATE locations SET category = 'EMERGENCY' WHERE category = '緊急'"
            );
            database.execSQL(
                    "UPDATE locations SET category = 'WARNING' WHERE category = '警告'"
            );
            database.execSQL(
                    "UPDATE locations SET category = 'INFO' WHERE category = '情報'"
            );
            database.execSQL(
                    "UPDATE locations SET category = 'SUPPORT' WHERE category = '支援'"
            );
        }
    };

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {

            // 1. 新テーブル作成（default を DAILY に）
            db.execSQL(
                    "CREATE TABLE locations_new (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "latitude REAL NOT NULL, " +
                            "longitude REAL NOT NULL, " +
                            "timestamp TEXT, " +
                            "memo TEXT, " +
                            "photoUri TEXT, " +
                            "category TEXT DEFAULT 'DAILY', " +
                            "subCategory TEXT" +
                            ")"
            );

            // 2. データコピー
            db.execSQL(
                    "INSERT INTO locations_new " +
                            "(id, latitude, longitude, timestamp, memo, photoUri, category, subCategory) " +
                            "SELECT id, latitude, longitude, timestamp, memo, photoUri, category, subCategory " +
                            "FROM locations"
            );

            // 3. 古いテーブル削除
            db.execSQL("DROP TABLE locations");

            // 4. リネーム
            db.execSQL("ALTER TABLE locations_new RENAME TO locations");
        }
    };

}