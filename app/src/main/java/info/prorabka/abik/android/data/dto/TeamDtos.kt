package info.prorabka.abik.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TeamDetailDto(
    val id: Long,
    val name: String,
    val cityId: Long? = null,
    val cityName: String? = null,
    val homeRegionId: Long? = null,
    val homeRegionName: String? = null,
    val logoUrl: String? = null,
    val description: String? = null,
    val season: String? = null,
    val status: String,
    val myRole: String? = null,
    val membersCount: Long = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val archivedAt: String? = null,
)

@Serializable
data class TeamCreateRequestDto(
    val name: String,
    val cityId: Long,
    val homeRegionId: Long,
    val logoUrl: String,
    val description: String? = null,
    val season: String? = null,
)

@Serializable
data class TeamMemberDto(
    val id: Long,
    val userId: Long,
    val phone: String? = null,
    val lastName: String? = null,
    val firstName: String? = null,
    val role: String,
    val status: String,
    val jerseyNumber: Int? = null,
    val isTrainer: Boolean = false,
    val joinedAt: String? = null,
)

@Serializable
data class InviteRequestDto(val phone: String)

@Serializable
data class InvitationDto(
    val id: Long,
    val teamId: Long,
    val teamName: String? = null,
    val inviterUserId: Long? = null,
    val invitedUserId: Long? = null,
    val invitedPhone: String? = null,
    val status: String,
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val respondedAt: String? = null,
)

@Serializable
data class UpdateMemberRoleRequestDto(val role: String)

@Serializable
data class SetJerseyNumberRequestDto(val jerseyNumber: Int?)

@Serializable
data class SetTrainerRequestDto(val isTrainer: Boolean)

/** BE-07: карточка участника команды (членство + профиль). */
@Serializable
data class MemberDetailsDto(
    val userId: Long,
    val phone: String? = null,
    val role: String,
    val membershipStatus: String,
    val jerseyNumber: Int? = null,
    val isTrainer: Boolean = false,
    val joinedAt: String? = null,
    val profile: UserProfileResponse? = null,
)

@Serializable
data class TransferOwnershipRequestDto(val newOwnerUserId: Long)
