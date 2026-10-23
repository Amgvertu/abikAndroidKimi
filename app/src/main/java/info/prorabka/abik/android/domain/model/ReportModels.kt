package info.prorabka.abik.android.domain.model

/** Сводка по команде: начислено / оплачено / остаток / общий баланс. */
data class TeamSummary(
    val totalChargesKopecks: Long,
    val totalPaidKopecks: Long,
    val totalRemainingKopecks: Long,
    val totalBalanceKopecks: Long,
    val activeChargesCount: Long,
    val paidChargesCount: Long,
)

data class MemberSummary(
    val userId: Long,
    val name: String,
    val totalChargesKopecks: Long,
    val totalPaidKopecks: Long,
    val totalRemainingKopecks: Long,
    val balanceKopecks: Long,
)
