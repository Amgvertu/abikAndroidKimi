package info.prorabka.abik.android.domain.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.ExpenseFormData
import info.prorabka.abik.android.domain.model.TeamExpense

interface ExpenseRepository {
    suspend fun list(teamId: Long): ApiResult<List<TeamExpense>>
    suspend fun create(teamId: Long, data: ExpenseFormData): ApiResult<Unit>
    suspend fun update(teamId: Long, id: Long, data: ExpenseFormData): ApiResult<Unit>
    suspend fun delete(teamId: Long, id: Long): ApiResult<Unit>
}
