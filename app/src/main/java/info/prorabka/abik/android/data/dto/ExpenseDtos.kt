package info.prorabka.abik.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExpenseDto(
    val id: Long,
    val category: String,
    val amountKopecks: Long,
    val expenseDate: String,
    val comment: String? = null,
    val eventId: Long? = null,
    val createdBy: Long,
)

@Serializable
data class ExpenseRequestDto(
    val category: String,
    val amountKopecks: Long,
    val expenseDate: String,
    val comment: String? = null,
    val eventId: Long? = null,
)
