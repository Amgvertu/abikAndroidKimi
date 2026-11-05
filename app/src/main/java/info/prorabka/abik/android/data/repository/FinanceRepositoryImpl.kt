package info.prorabka.abik.android.data.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.BalanceApi
import info.prorabka.abik.android.data.api.PaymentApi
import info.prorabka.abik.android.data.dto.BalanceAdjustmentRequestDto
import info.prorabka.abik.android.data.dto.PaymentCreateRequestDto
import info.prorabka.abik.android.data.dto.PaymentUpdateRequestDto
import info.prorabka.abik.android.domain.model.Balance
import info.prorabka.abik.android.domain.model.BalanceAdjustment
import info.prorabka.abik.android.domain.model.Payment
import info.prorabka.abik.android.domain.repository.FinanceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepositoryImpl @Inject constructor(
    private val balanceApi: BalanceApi,
    private val paymentApi: PaymentApi,
    private val queue: info.prorabka.abik.android.data.local.OfflineQueue,
    private val cache: info.prorabka.abik.android.data.local.CacheStore,
    private val json: kotlinx.serialization.json.Json,
    private val tokenStore: info.prorabka.abik.android.core.prefs.TokenStore,
) : FinanceRepository {

    override suspend fun balances(teamId: Long): ApiResult<List<Balance>> {
        val key = "${tokenStore.userId}:team:$teamId:balances"
        val serializer = kotlinx.serialization.builtins.ListSerializer(
            info.prorabka.abik.android.data.dto.BalanceDto.serializer())
        return when (val result = apiCall { balanceApi.list(teamId) }) {
            is ApiResult.Success -> {
                cache.write(key, serializer, result.data)
                ApiResult.Success(result.data.map { it.toDomain() })
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(key, serializer)
                if (cached != null) ApiResult.Success(cached.map { it.toDomain() }) else result
            }
            is ApiResult.HttpError -> result
        }
    }

    override suspend fun balanceOf(teamId: Long, userId: Long): ApiResult<Balance> {
        val key = "${tokenStore.userId}:team:$teamId:balance:$userId"
        val serializer = info.prorabka.abik.android.data.dto.BalanceDto.serializer()
        return when (val result = apiCall { balanceApi.get(teamId, userId) }) {
            is ApiResult.Success -> {
                cache.write(key, serializer, result.data)
                ApiResult.Success(result.data.toDomain())
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(key, serializer)
                if (cached != null) return ApiResult.Success(cached.toDomain())
                val listKey = "${tokenStore.userId}:team:$teamId:balances"
                val fromList = cache.read(listKey,
                    kotlinx.serialization.builtins.ListSerializer(serializer))
                    ?.firstOrNull { it.userId == userId }
                if (fromList != null) ApiResult.Success(fromList.toDomain()) else result
            }
            is ApiResult.HttpError -> result
        }
    }

    override suspend fun adjustBalance(
        teamId: Long,
        userId: Long,
        amountKopecks: Long,
        reason: String,
    ): ApiResult<Unit> = apiCall {
        balanceApi.adjust(teamId, userId, BalanceAdjustmentRequestDto(amountKopecks, reason))
        Unit
    }

    override suspend fun adjustments(
        teamId: Long,
        userId: Long,
    ): ApiResult<List<BalanceAdjustment>> = apiCall {
        balanceApi.adjustments(teamId, userId).map {
            BalanceAdjustment(it.id, it.amountKopecks, it.reason ?: "", it.createdAt)
        }
    }

    override suspend fun createPayment(
        teamId: Long,
        userId: Long,
        amountKopecks: Long,
        paidAtIso: String?,
        description: String?,
    ): ApiResult<info.prorabka.abik.android.domain.model.SyncOutcome> {
        val body = PaymentCreateRequestDto(userId, amountKopecks, paidAtIso, description)
        return when (val result = apiCall { paymentApi.create(teamId, body) }) {
            is ApiResult.Success -> ApiResult.Success(info.prorabka.abik.android.domain.model.SyncOutcome.SYNCED)
            is ApiResult.NetworkError -> {
                queue.enqueue(teamId, info.prorabka.abik.android.data.local.OfflineOperationTypes.PAYMENT_CREATE,
                    body, PaymentCreateRequestDto.serializer())
                ApiResult.Success(info.prorabka.abik.android.domain.model.SyncOutcome.PENDING)
            }
            is ApiResult.HttpError -> result
        }
    }

    override suspend fun payments(
        teamId: Long,
        userId: Long?,
        dateFromIso: String?,
        dateToIso: String?,
    ): ApiResult<List<Payment>> {
        val key = "${tokenStore.userId}:team:$teamId:payments:$userId:$dateFromIso:$dateToIso"
        val serializer = kotlinx.serialization.builtins.ListSerializer(
            info.prorabka.abik.android.data.dto.PaymentDto.serializer())
        val local = queue.forType(teamId, info.prorabka.abik.android.data.local.OfflineOperationTypes.PAYMENT_CREATE)
        when (val result = apiCall { paymentApi.list(teamId, userId, dateFromIso, dateToIso) }) {
            is ApiResult.Success -> {
                cache.write(key, serializer, result.data)
                queue.clearSynced(teamId, info.prorabka.abik.android.data.local.OfflineOperationTypes.PAYMENT_CREATE)
                return ApiResult.Success(result.data.map { it.toDomain() } +
                    pendingPayments(teamId, local.filter { it.isLocalOnly }, userId, dateFromIso, dateToIso))
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(key, serializer).orEmpty()
                if (cached.isEmpty() && local.isEmpty()) return result
                return ApiResult.Success(cached.map { it.toDomain() } +
                    pendingPayments(teamId, local, userId, dateFromIso, dateToIso))
            }
            is ApiResult.HttpError -> return result
        }
    }

    override suspend fun paymentById(teamId: Long, paymentId: Long): ApiResult<Payment> = apiCall {
        val dto = paymentApi.get(teamId, paymentId)
        Payment(
            id = dto.id,
            teamId = dto.teamId,
            userId = dto.userId,
            userName = listOfNotNull(dto.userLastName, dto.userFirstName).joinToString(" "),
            amountKopecks = dto.amountKopecks,
            description = dto.description,
            paidAt = dto.paidAt ?: dto.createdAt,
            createdByName = listOfNotNull(dto.createdByLastName, dto.createdByFirstName)
                .joinToString(" ")
                .ifBlank { null },
            createdAt = dto.createdAt,
        )
    }

    override suspend fun updatePayment(
        teamId: Long,
        paymentId: Long,
        userId: Long?,
        amountKopecks: Long?,
        paidAtIso: String?,
        description: String?,
    ): ApiResult<Unit> = apiCall {
        paymentApi.update(
            teamId,
            paymentId,
            PaymentUpdateRequestDto(userId, amountKopecks, paidAtIso, description),
        )
        Unit
    }

    override suspend fun deletePayment(teamId: Long, paymentId: Long): ApiResult<Unit> = apiCall {
        paymentApi.delete(teamId, paymentId)
        Unit
    }

    private suspend fun pendingPayments(
        teamId: Long,
        operations: List<info.prorabka.abik.android.data.local.OfflineOperationEntity>,
        userId: Long?,
        dateFrom: String?,
        dateTo: String?,
    ): List<Payment> {
        val members = cache.read("${tokenStore.userId}:team:$teamId:members",
            kotlinx.serialization.builtins.ListSerializer(
                info.prorabka.abik.android.data.dto.TeamMemberDto.serializer())).orEmpty()
        return operations.mapNotNull { operation ->
            val body = runCatching {
                json.decodeFromString(PaymentCreateRequestDto.serializer(), operation.payloadJson)
            }.getOrNull() ?: return@mapNotNull null
            if (userId != null && body.userId != userId) return@mapNotNull null
            val date = body.paidAt ?: operation.localCreatedAt.take(10)
            if (dateFrom != null && date < dateFrom) return@mapNotNull null
            if (dateTo != null && date > dateTo) return@mapNotNull null
            val member = members.firstOrNull { it.userId == body.userId }
            Payment(
                id = localId(operation.operationId), teamId = teamId, userId = body.userId,
                userName = listOfNotNull(member?.lastName, member?.firstName).joinToString(" "),
                amountKopecks = body.amountKopecks, description = body.description,
                paidAt = body.paidAt ?: operation.localCreatedAt,
                createdByName = null, createdAt = operation.localCreatedAt,
                pendingSync = operation.isLocalOnly, syncError = operation.displaySyncError,
            )
        }
    }
}

private fun info.prorabka.abik.android.data.dto.BalanceDto.toDomain() = Balance(
    userId = userId,
    userName = listOfNotNull(userLastName, userFirstName)
        .joinToString(" ")
        .ifBlank { "ID $userId" },
    amountKopecks = amountKopecks,
)

private fun info.prorabka.abik.android.data.dto.PaymentDto.toDomain() = Payment(
    id = id, teamId = teamId, userId = userId,
    userName = listOfNotNull(userLastName, userFirstName).joinToString(" "),
    amountKopecks = amountKopecks, description = description, paidAt = paidAt ?: createdAt,
    createdByName = listOfNotNull(createdByLastName, createdByFirstName).joinToString(" ").ifBlank { null },
    createdAt = createdAt,
)
