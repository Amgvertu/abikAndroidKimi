package info.prorabka.abik.android.data.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.ExpenseApi
import info.prorabka.abik.android.data.dto.ExpenseDto
import info.prorabka.abik.android.data.dto.ExpenseRequestDto
import info.prorabka.abik.android.domain.model.ExpenseFormData
import info.prorabka.abik.android.domain.model.TeamExpense
import info.prorabka.abik.android.domain.repository.ExpenseRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val api: ExpenseApi,
    private val queue: info.prorabka.abik.android.data.local.OfflineQueue,
    private val cache: info.prorabka.abik.android.data.local.CacheStore,
    private val json: kotlinx.serialization.json.Json,
    private val tokenStore: info.prorabka.abik.android.core.prefs.TokenStore,
) : ExpenseRepository {
    override suspend fun list(teamId: Long): ApiResult<List<TeamExpense>> {
        val key = "${tokenStore.userId}:team:$teamId:expenses"
        val serializer = kotlinx.serialization.builtins.ListSerializer(ExpenseDto.serializer())
        val local = queue.forType(teamId, info.prorabka.abik.android.data.local.OfflineOperationTypes.EXPENSE_CREATE)
        when (val result = apiCall { api.list(teamId) }) {
            is ApiResult.Success -> {
                cache.write(key, serializer, result.data)
                queue.clearSynced(teamId, info.prorabka.abik.android.data.local.OfflineOperationTypes.EXPENSE_CREATE)
                return ApiResult.Success(result.data.map { it.toDomain() } + pendingExpenses(local.filter { it.isLocalOnly }))
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(key, serializer).orEmpty()
                if (cached.isEmpty() && local.isEmpty()) return result
                return ApiResult.Success(cached.map { it.toDomain() } + pendingExpenses(local))
            }
            is ApiResult.HttpError -> return result
        }
    }

    override suspend fun create(teamId: Long, data: ExpenseFormData): ApiResult<info.prorabka.abik.android.domain.model.SyncOutcome> {
        return when (val result = apiCall { api.create(teamId, data.toDto()) }) {
            is ApiResult.Success -> ApiResult.Success(info.prorabka.abik.android.domain.model.SyncOutcome.SYNCED)
            is ApiResult.NetworkError -> {
                queue.enqueue(teamId, info.prorabka.abik.android.data.local.OfflineOperationTypes.EXPENSE_CREATE,
                    data.toDto(), ExpenseRequestDto.serializer())
                ApiResult.Success(info.prorabka.abik.android.domain.model.SyncOutcome.PENDING)
            }
            is ApiResult.HttpError -> result
        }
    }

    override suspend fun update(teamId: Long, id: Long, data: ExpenseFormData) = apiCall {
        api.update(teamId, id, data.toDto()); Unit
    }
    override suspend fun delete(teamId: Long, id: Long) = apiCall { api.delete(teamId, id) }

    private fun pendingExpenses(operations: List<info.prorabka.abik.android.data.local.OfflineOperationEntity>) =
        operations.mapNotNull { operation ->
            val body = runCatching {
                json.decodeFromString(ExpenseRequestDto.serializer(), operation.payloadJson)
            }.getOrNull() ?: return@mapNotNull null
            TeamExpense(
                id = localId(operation.operationId), category = body.category,
                amountKopecks = body.amountKopecks, expenseDate = body.expenseDate,
                comment = body.comment, eventId = body.eventId, createdBy = operation.userId,
                pendingSync = operation.isLocalOnly, syncError = operation.displaySyncError,
            )
        }
}

private fun ExpenseDto.toDomain() = TeamExpense(id, category, amountKopecks, expenseDate, comment, eventId, createdBy)
private fun ExpenseFormData.toDto() = ExpenseRequestDto(category, amountKopecks, expenseDate, comment, eventId)
