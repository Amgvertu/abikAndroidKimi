package info.prorabka.abik.android.data.local

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheStore @Inject constructor(
    private val dao: CacheEntryDao,
    private val json: Json,
) {
    suspend fun <T> write(key: String, serializer: KSerializer<T>, value: T) {
        dao.put(CacheEntryEntity(key, json.encodeToString(serializer, value)))
    }

    suspend fun <T> read(key: String, serializer: KSerializer<T>): T? =
        dao.get(key)?.json?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
}
