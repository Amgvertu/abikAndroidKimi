package info.prorabka.abik.android.data.local

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import info.prorabka.abik.android.core.prefs.TokenStore
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineQueue @Inject constructor(
    private val dao: OfflineOperationDao,
    private val tokenStore: TokenStore,
    private val json: Json,
    @ApplicationContext private val context: Context,
) {
    suspend fun <T> enqueue(
        teamId: Long,
        type: String,
        payload: T,
        serializer: KSerializer<T>,
        expectedEventRevision: Long? = null,
        eventId: Long? = null,
        replacePendingEvent: Boolean = false,
    ): String {
        val userId = tokenStore.userId
        require(userId > 0) { "Нельзя сохранить офлайн-операцию без авторизации" }
        val id = UUID.randomUUID().toString()
        val element = json.encodeToJsonElement(serializer, payload)
        require(element is JsonObject) { "Полезная нагрузка должна быть JSON-объектом" }
        if (replacePendingEvent && eventId != null) {
            dao.deletePendingEvent(userId, teamId, type, eventId)
        }
        dao.insert(
            OfflineOperationEntity(
                operationId = id,
                userId = userId,
                teamId = teamId,
                type = type,
                eventId = eventId,
                payloadJson = element.toString(),
                expectedEventRevision = expectedEventRevision,
            ),
        )
        schedule(userId)
        return id
    }

    suspend fun forType(teamId: Long, type: String): List<OfflineOperationEntity> =
        dao.forType(tokenStore.userId, teamId, type)

    suspend fun forTypes(teamId: Long, types: List<String>): List<OfflineOperationEntity> =
        dao.forTypes(tokenStore.userId, teamId, types)

    suspend fun clearSynced(teamId: Long, type: String) =
        dao.deleteSynced(tokenStore.userId, teamId, type)

    suspend fun clearSyncedEvent(teamId: Long, type: String, eventId: Long) =
        dao.deleteSyncedEvent(tokenStore.userId, teamId, type, eventId)

    suspend fun clearEventOperations(teamId: Long, type: String, eventId: Long) =
        dao.deleteEventOperations(tokenStore.userId, teamId, type, eventId)

    fun schedule(userId: Long = tokenStore.userId) {
        if (userId <= 0) return
        val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "offline-sync-v1-$userId",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
