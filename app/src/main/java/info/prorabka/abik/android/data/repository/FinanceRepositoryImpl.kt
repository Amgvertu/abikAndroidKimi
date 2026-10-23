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
) : FinanceRepository {

    override suspend fun balances(teamId: Long): ApiResult<List<Balance>> = apiCall {
        balanceApi.list(teamId).map {
            Balance(
                userId = it.userId,
                userName = listOfNotNull(it.userLastName, it.userFirstName)
                    .joinToString(" ")
                    .ifBlank { "ID ${it.userId}" },
                amountKopecks = it.amountKopecks,
            )
        }
    }

    override suspend fun balanceOf(teamId: Long, userId: Long): ApiResult<Balance> = apiCall {
        val dto = balanceApi.get(teamId, userId)
        Balance(
            userId = dto.userId,
            userName = listOfNotNull(dto.userLastName, dto.userFirstName).joinToString(" "),
            amountKopecks = dto.amountKopecks,
        )
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
    ): ApiResult<Unit> = apiCall {
        paymentApi.create(teamId, PaymentCreateRequestDto(userId, amountKopecks, paidAtIso, description))
        Unit
    }

    override suspend fun payments(
        teamId: Long,
        userId: Long?,
        dateFromIso: String?,
        dateToIso: String?,
    ): ApiResult<List<Payment>> = apiCall {
        paymentApi.list(teamId, userId, dateFromIso, dateToIso).map { dto ->
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
}
