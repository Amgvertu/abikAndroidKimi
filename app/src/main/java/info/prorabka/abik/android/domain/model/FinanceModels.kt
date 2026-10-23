package info.prorabka.abik.android.domain.model

/** Баланс участника: отрицательный — долг, положительный — переплата. */
data class Balance(
    val userId: Long,
    val userName: String,
    val amountKopecks: Long,
) {
    val isDebt: Boolean get() = amountKopecks < 0
}

data class Payment(
    val id: Long,
    val teamId: Long,
    val userId: Long,
    val userName: String,
    val amountKopecks: Long,
    val description: String?,
    val paidAt: String?,
    val createdByName: String?,
    val createdAt: String?,
)

data class BalanceAdjustment(
    val id: Long,
    val amountKopecks: Long,
    val reason: String,
    val createdAt: String?,
)
