package info.prorabka.abik.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache_entries")
data class CacheEntryEntity(
    @PrimaryKey val cacheKey: String,
    val json: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
