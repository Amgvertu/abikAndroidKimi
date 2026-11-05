package info.prorabka.abik.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [OfflineOperationEntity::class, CacheEntryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AbikDatabase : RoomDatabase() {
    abstract fun offlineOperations(): OfflineOperationDao
    abstract fun cacheEntries(): CacheEntryDao
}
