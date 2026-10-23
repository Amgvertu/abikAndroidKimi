package info.prorabka.abik.android.domain.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.Balance
import info.prorabka.abik.android.domain.model.BalanceAdjustment
import info.prorabka.abik.android.domain.model.Payment

interface FinanceRepository {
    suspend fun balances(teamId: Long): ApiResult<List<Balance>>
    suspend fun balanceOf(teamId: Long, userId: Long): ApiResult<Balance>
    suspend fun adjustBalance(teamId: Long, userId: Long, amountKopecks: Long, reason: String): ApiResult<Unit>
    suspend fun adjustments(teamId: Long, userId: Long): ApiResult<List<BalanceAdjustment>>

    suspend fun createPayment(
        teamId: Long,
        userId: Long,
        amountKopecks: Long,
        paidAtIso: String?,
        description: String?,
    ): ApiResult<Unit>

    suspend fun payments(
        teamId: Long,
        userId: Long? = null,
        dateFromIso: String? = null,
        dateToIso: String? = null,
    ): ApiResult<List<Payment>>

    suspend fun updatePayment(
        teamId: Long,
        paymentId: Long,
        userId: Long?,
        amountKopecks: Long?,
        paidAtIso: String?,
        description: String?,
    ): ApiResult<Unit>

    suspend fun deletePayment(teamId: Long, paymentId: Long): ApiResult<Unit>
}
