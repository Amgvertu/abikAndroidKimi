package info.prorabka.abik.android.data.local

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.core.prefs.TokenStore
import info.prorabka.abik.android.data.api.OfflineApi
import info.prorabka.abik.android.data.dto.OfflineOperationRequestDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: OfflineOperationDao,
    private val api: OfflineApi,
    private val tokenStore: TokenStore,
    private val json: Json,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = tokenStore.userId
        if (userId <= 0 || !tokenStore.isLoggedIn) return Result.success()

        while (true) {
            val operation = dao.nextPending(userId) ?: return Result.success()
            val request = OfflineOperationRequestDto(
                operationId = operation.operationId,
                type = operation.type,
                payload = json.parseToJsonElement(operation.payloadJson).jsonObject,
                expectedEventRevision = operation.expectedEventRevision,
            )
            when (val result = apiCall { api.execute(operation.teamId, request) }) {
                is ApiResult.Success -> dao.update(operation.copy(
                    status = OfflineOperationEntity.STATUS_SYNCED,
                    lastError = null,
                ))
                is ApiResult.NetworkError -> {
                    dao.update(operation.copy(
                        attempts = operation.attempts + 1,
                        lastError = "Сервер недоступен; повторим автоматически",
                    ))
                    return Result.retry()
                }
                is ApiResult.HttpError -> {
                    if (result.httpCode == 401 || result.httpCode == 408 ||
                        result.httpCode == 429 || result.httpCode >= 500
                    ) {
                        dao.update(operation.copy(
                            attempts = operation.attempts + 1,
                            lastError = result.message,
                        ))
                        return Result.retry()
                    }
                    val conflict = result.code == "OFFLINE_CONFLICT" || result.httpCode == 409
                    dao.update(operation.copy(
                        status = if (conflict) {
                            OfflineOperationEntity.STATUS_CONFLICT
                        } else {
                            OfflineOperationEntity.STATUS_FAILED
                        },
                        attempts = operation.attempts + 1,
                        lastError = result.message,
                    ))
                }
            }
        }
    }
}
