package info.prorabka.abik.android.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class OfflineOperationRequestDto(
    val operationId: String,
    val type: String,
    val payload: JsonObject,
    val expectedEventRevision: Long? = null,
)

@Serializable
data class OfflineChargeCreateRequestDto(
    val groupId: Long? = null,
    val groupName: String? = null,
    val subgroupId: Long? = null,
    val subgroupName: String? = null,
    val userIds: Set<Long>,
    val amountKopecks: Long,
    val description: String? = null,
    val dueDate: String,
)

@Serializable
data class OfflineLineupRequestDto(
    val eventId: Long,
    val slots: List<LineupSlotDto>,
)

@Serializable
data class OfflineSweatersRequestDto(
    val eventId: Long,
    val items: List<SweaterColorItemDto>,
)
