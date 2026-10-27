package info.prorabka.abik.android.data.dto

import kotlinx.serialization.Serializable

// --- Состав регулярки (roster), BE-09 ---

@Serializable
data class SeasonDto(
    val season: String,
    val current: Boolean = false,
)

@Serializable
data class RosterItemDto(
    val userId: Long,
    val fullName: String? = null,
    val positions: Set<String> = emptySet(),
    val status: String,
    val addedAt: String? = null,
)

@Serializable
data class EntryFeeDto(
    val groupId: Long,
    val subgroupId: Long? = null,
    val amountKopecks: Long,
    val dueDate: String,
)

@Serializable
data class RosterAddRequestDto(
    val userId: Long,
    val chargeEntryFee: Boolean = false,
    val entryFee: EntryFeeDto? = null,
)

@Serializable
data class RosterReplaceRequestDto(val userIds: Set<Long>)
