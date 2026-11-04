package info.prorabka.abik.android.domain.model

data class TeamExpense(
    val id: Long,
    val category: String,
    val amountKopecks: Long,
    val expenseDate: String,
    val comment: String?,
    val eventId: Long?,
    val createdBy: Long,
)

data class ExpenseFormData(
    val category: String,
    val amountKopecks: Long,
    val expenseDate: String,
    val comment: String?,
    val eventId: Long? = null,
)
