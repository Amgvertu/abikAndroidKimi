package info.prorabka.abik.android.data.local

import androidx.room.*

@Dao
interface CacheEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: CacheEntryEntity)

    @Query("SELECT * FROM cache_entries WHERE cacheKey = :key")
    suspend fun get(key: String): CacheEntryEntity?
}
