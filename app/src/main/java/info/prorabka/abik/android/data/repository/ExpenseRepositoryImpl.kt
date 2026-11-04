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
class ExpenseRepositoryImpl @Inject constructor(private val api: ExpenseApi) : ExpenseRepository {
    override suspend fun list(teamId: Long) = apiCall { api.list(teamId).map { it.toDomain() } }
    override suspend fun create(teamId: Long, data: ExpenseFormData) = apiCall {
        api.create(teamId, data.toDto()); Unit
    }
    override suspend fun update(teamId: Long, id: Long, data: ExpenseFormData) = apiCall {
        api.update(teamId, id, data.toDto()); Unit
    }
    override suspend fun delete(teamId: Long, id: Long) = apiCall { api.delete(teamId, id) }
}

private fun ExpenseDto.toDomain() = TeamExpense(id, category, amountKopecks, expenseDate, comment, eventId, createdBy)
private fun ExpenseFormData.toDto() = ExpenseRequestDto(category, amountKopecks, expenseDate, comment, eventId)
