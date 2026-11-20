package info.prorabka.abik.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChargeDto(
    val id: Long,
    val teamId: Long,
    val templateId: Long? = null,
    val groupId: Long? = null,
    val groupName: String? = null,
    val subgroupId: Long? = null,
    val subgroupName: String? = null,
    val userId: Long,
    val userLastName: String? = null,
    val userFirstName: String? = null,
    val amountKopecks: Long,
    val paidKopecks: Long = 0,
    val remainingKopecks: Long = 0,
    val status: String,
    val description: String? = null,
    val dueDate: String? = null,
    val periodYear: Int? = null,
    val periodMonth: Int? = null,
    val createdAt: String? = null,
    val cancelledAt: String? = null,
    val cancelReason: String? = null,
)

@Serializable
data class ChargeCreateManualRequestDto(
    val groupId: Long,
    val subgroupId: Long? = null,
    val userIds: Set<Long>,
    val amountKopecks: Long,
    val description: String? = null,
    val dueDate: String,
)

@Serializable
data class ChargeCancelRequestDto(val reason: String)

@Serializable
data class ChargeUpdateRequestDto(
    val groupId: Long,
    val subgroupId: Long? = null,
    val amountKopecks: Long,
    val description: String? = null,
    val dueDate: String,
)

@Serializable
data class ChargeGroupDto(
    val id: Long,
    val teamId: Long,
    val name: String,
)

@Serializable
data class ChargeSubgroupDto(
    val id: Long,
    val groupId: Long,
    val name: String,
)

@Serializable
data class ChargeGroupRequestDto(val name: String)

@Serializable
data class ChargeSubgroupRequestDto(val name: String)

// --- Шаблоны начислений ---

@Serializable
data class ChargeTemplateDto(
    val id: Long,
    val teamId: Long,
    val groupId: Long,
    val subgroupId: Long? = null,
    val name: String,
    val amountKopecks: Long,
    val type: String,
    val periodicity: String,
    val dayOfMonth: Int? = null,
    val pollEnabled: Boolean = false,
    val pollStartDay: Int? = null,
    val pollEndDay: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val distribution: String,
    val active: Boolean = true,
    val memberIds: Set<Long> = emptySet(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class ChargeTemplateRequestDto(
    val groupId: Long,
    val subgroupId: Long? = null,
    val name: String,
    val amountKopecks: Long,
    val type: String,
    val periodicity: String,
    val dayOfMonth: Int? = null,
    val pollEnabled: Boolean = false,
    val pollStartDay: Int? = null,
    val pollEndDay: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val distribution: String,
    val memberIds: Set<Long>,
    val active: Boolean? = null,
)

// --- Именованные списки получателей начислений ---

@Serializable
data class ChargeListDto(
    val id: Long,
    val name: String,
    val memberIds: Set<Long> = emptySet(),
)

@Serializable
data class ChargeListRequestDto(
    val name: String,
    val memberIds: Set<Long>,
)


@Serializable
data class ActiveSubscriptionDto(
    val teamId: Long,
    val teamName: String,
    val title: String,
    val validFrom: String,
    val validUntil: String,
)

@Serializable
data class SubscriptionPollVoterDto(val userId: Long, val name: String)

@Serializable
data class SubscriptionPollDto(
    val id: Long,
    val templateId: Long,
    val title: String,
    val startDate: String,
    val endDate: String,
    val validFrom: String,
    val validUntil: String,
    val yesCount: Int,
    val noCount: Int,
    val myVote: String? = null,
    val positiveVoters: List<SubscriptionPollVoterDto> = emptyList(),
)

@Serializable
data class SubscriptionPollVoteRequestDto(val answer: String)
