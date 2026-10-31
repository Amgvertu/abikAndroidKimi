package info.prorabka.abik.android.data.dto

import kotlinx.serialization.Serializable

// --- События (AND-06/07), BE-11…BE-13 ---

@Serializable
data class EventListItemDto(
    val id: Long,
    val title: String = "",
    val section: String = "TRAINING",
    val type: String,
    val date: String,
    val time: String,
    val place: String,
    val myVote: String? = null,
    val votesYes: Long = 0,
    val votesNo: Long = 0,
    val votesMaybe: Long = 0,
    val requiredPlayers: Int? = null,
    val votesYesGoalies: Int = 0,
    val votesYesDefenders: Int = 0,
    val votesYesForwards: Int = 0,
    val votesYesReserve: Int? = null,
)

@Serializable
data class EventVoterDto(
    val userId: Long,
    val fullName: String? = null,
    val answer: String? = null,
    val positions: Set<String> = emptySet(),
    val isTrainer: Boolean = false,
    val sweaterColor: String? = null,
    val inReserve: Boolean = false,
)

@Serializable
data class LineupSlotDto(
    val lineNumber: Int? = null,
    val slotNumber: Int,
    val role: String,
    val userId: Long,
)

@Serializable
data class LineupDto(
    val slots: List<LineupSlotDto> = emptyList(),
)

@Serializable
data class LineupSaveRequestDto(
    val slots: List<LineupSlotDto>,
)

@Serializable
data class EventDetailDto(
    val id: Long,
    val title: String = "",
    val section: String = "TRAINING",
    val type: String,
    val date: String,
    val time: String,
    val place: String,
    val note: String? = null,
    val status: String = "ACTIVE",
    val seriesId: Long? = null,
    val myVote: String? = null,
    val voters: List<EventVoterDto> = emptyList(),
    val lineup: LineupDto? = null,
    val requiredPlayers: Int? = null,
    val chargeMode: String? = null,
    val chargeAmountKopecks: Long? = null,
    val chargeDistribution: String? = null,
    val chargeTemplateId: Long? = null,
    val charged: Boolean = false,
)

@Serializable
data class RecurrenceDto(
    val weekdays: Set<Int>,
    val untilDate: String,
)

@Serializable
data class EventCreateRequestDto(
    val title: String,
    val section: String,
    val type: String,
    val date: String,
    val time: String,
    val place: String,
    val note: String? = null,
    val recurrence: RecurrenceDto? = null,
    val requiredPlayers: Int? = null,
    val chargeMode: String? = null,
    val chargeAmountKopecks: Long? = null,
    val chargeDistribution: String? = null,
    val chargeTemplateId: Long? = null,
)

@Serializable
data class EventUpdateRequestDto(
    val title: String,
    val section: String,
    val type: String,
    val date: String,
    val time: String,
    val place: String,
    val note: String? = null,
    val requiredPlayers: Int? = null,
    val chargeMode: String? = null,
    val chargeAmountKopecks: Long? = null,
    val chargeDistribution: String? = null,
    val chargeTemplateId: Long? = null,
)

@Serializable
data class VoteRequestDto(
    val answer: String,
)

@Serializable
data class SweaterColorItemDto(
    val userId: Long,
    val color: String? = null,
)
