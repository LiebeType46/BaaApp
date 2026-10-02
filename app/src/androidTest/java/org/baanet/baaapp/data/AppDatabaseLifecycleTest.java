package org.baanet.baaapp.data;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseLifecycleTest {
    @Test
    public void checkpointKeepsDatabaseUsableForNextLogin() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppDatabase database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        try {
            LocationEntity location = new LocationEntity(
                    "DAILY", null, 35.0, 139.0, "2026/10/02 16:00:00", "test", false, null);
            database.locationDao().insert(location);

            database.checkpoint();

            assertTrue(database.isOpen());
            assertEquals(1, database.locationDao().claimUnowned("next-user"));
            assertEquals(1, database.locationDao().getAllLocationsLatestFirstByOwner("next-user").size());

            database.checkpoint();
            assertEquals(1, database.locationDao().getAllLocationsLatestFirstByOwner("next-user").size());
        } finally {
            database.close();
        }
    }
}
