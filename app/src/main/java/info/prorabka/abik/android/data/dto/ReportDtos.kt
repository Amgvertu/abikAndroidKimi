package info.prorabka.abik.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChargeSummaryDto(
    val teamId: Long,
    val totalChargesKopecks: Long = 0,
    val totalPaidKopecks: Long = 0,
    val totalRemainingKopecks: Long = 0,
    val totalBalanceKopecks: Long = 0,
    val activeChargesCount: Long = 0,
    val paidChargesCount: Long = 0,
    val partiallyPaidChargesCount: Long = 0,
    val cancelledChargesCount: Long = 0,
)

@Serializable
data class MemberSummaryDto(
    val userId: Long,
    val lastName: String? = null,
    val firstName: String? = null,
    val phone: String? = null,
    val totalChargesKopecks: Long = 0,
    val totalPaidKopecks: Long = 0,
    val totalRemainingKopecks: Long = 0,
    val balanceKopecks: Long = 0,
)
