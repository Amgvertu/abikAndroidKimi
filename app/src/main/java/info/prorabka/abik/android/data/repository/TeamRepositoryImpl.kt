package info.prorabka.abik.android.data.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.InvitationApi
import info.prorabka.abik.android.data.api.TeamApi
import info.prorabka.abik.android.data.dto.InviteRequestDto
import info.prorabka.abik.android.data.dto.SetJerseyNumberRequestDto
import info.prorabka.abik.android.data.dto.SetTrainerRequestDto
import info.prorabka.abik.android.data.dto.TeamCreateRequestDto
import info.prorabka.abik.android.data.dto.TeamDetailDto
import info.prorabka.abik.android.data.dto.TransferOwnershipRequestDto
import info.prorabka.abik.android.data.dto.UpdateMemberRoleRequestDto
import info.prorabka.abik.android.data.dto.UserProfileRequest
import info.prorabka.abik.android.domain.model.Invitation
import info.prorabka.abik.android.domain.model.MemberDetails
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.MemberStatus
import info.prorabka.abik.android.domain.model.Position
import info.prorabka.abik.android.domain.model.ProfileFormData
import info.prorabka.abik.android.domain.model.Team
import info.prorabka.abik.android.domain.model.TeamCreateData
import info.prorabka.abik.android.domain.model.TeamDetail
import info.prorabka.abik.android.domain.model.TeamMember
import info.prorabka.abik.android.domain.repository.TeamRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepositoryImpl @Inject constructor(
    private val teamApi: TeamApi,
    private val invitationApi: InvitationApi,
    private val cache: info.prorabka.abik.android.data.local.CacheStore,
    private val tokenStore: info.prorabka.abik.android.core.prefs.TokenStore,
) : TeamRepository {

    private fun cacheKey(suffix: String) = "${tokenStore.userId}:$suffix"

    override suspend fun myTeams(): ApiResult<List<Team>> {
        when (val result = apiCall { teamApi.myTeams() }) {
            is ApiResult.Success -> {
                cache.write(cacheKey("teams"), kotlinx.serialization.builtins.ListSerializer(
                    info.prorabka.abik.android.data.dto.TeamDto.serializer()), result.data)
                return ApiResult.Success(result.data.filter { it.status != "ARCHIVED" }.map { it.toDomain() })
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(cacheKey("teams"), kotlinx.serialization.builtins.ListSerializer(
                    info.prorabka.abik.android.data.dto.TeamDto.serializer()))
                if (cached != null) return ApiResult.Success(cached.filter { it.status != "ARCHIVED" }.map { it.toDomain() })
                return result
            }
            is ApiResult.HttpError -> return result
        }
    }

    override suspend fun detail(teamId: Long): ApiResult<TeamDetail> {
        val key = cacheKey("team:$teamId")
        when (val result = apiCall { teamApi.detail(teamId) }) {
            is ApiResult.Success -> {
                cache.write(key, TeamDetailDto.serializer(), result.data)
                return ApiResult.Success(result.data.toDomain())
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(key, TeamDetailDto.serializer())
                return if (cached != null) ApiResult.Success(cached.toDomain()) else result
            }
            is ApiResult.HttpError -> return result
        }
    }

    override suspend fun create(data: TeamCreateData): ApiResult<TeamDetail> = apiCall {
        teamApi.create(data.toDto()).toDomain()
    }

    override suspend fun update(teamId: Long, data: TeamCreateData): ApiResult<TeamDetail> =
        apiCall {
            teamApi.update(teamId, data.toDto()).toDomain()
        }

    override suspend fun archive(teamId: Long): ApiResult<Unit> = apiCall {
        teamApi.archive(teamId)
        Unit
    }

    override suspend fun transferOwnership(teamId: Long, newOwnerUserId: Long): ApiResult<Unit> =
        apiCall {
            teamApi.transferOwnership(teamId, TransferOwnershipRequestDto(newOwnerUserId))
            Unit
        }

    override suspend fun members(teamId: Long): ApiResult<List<TeamMember>> {
        val key = cacheKey("team:$teamId:members")
        val serializer = kotlinx.serialization.builtins.ListSerializer(
            info.prorabka.abik.android.data.dto.TeamMemberDto.serializer())
        when (val result = apiCall { teamApi.members(teamId) }) {
            is ApiResult.Success -> {
                cache.write(key, serializer, result.data)
                return ApiResult.Success(result.data.map { it.toDomain() })
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(key, serializer)
                return if (cached != null) ApiResult.Success(cached.map { it.toDomain() }) else result
            }
            is ApiResult.HttpError -> return result
        }
    }

    override suspend fun updateMemberProfile(
        teamId: Long,
        userId: Long,
        data: ProfileFormData,
    ): ApiResult<Unit> = apiCall {
        teamApi.updateMemberProfile(
            teamId,
            userId,
            UserProfileRequest(
                cityId = data.cityId,
                lastName = data.lastName,
                firstName = data.firstName,
                middleName = data.middleName,
                birthDate = data.birthDateIso,
                heightCm = data.heightCm,
                weightKg = data.weightKg,
                stickHand = data.stickHand.name,
                positions = data.positions.map { it.name }.toSet(),
                playerStatus = data.playerStatus.name,
                jerseySize = data.jerseySize,
                coversSize = data.coversSize,
                gaitersSize = data.gaitersSize,
                avatarUrl = data.avatarUrl,
            ),
        )
        Unit
    }

    override suspend fun memberDetails(teamId: Long, userId: Long): ApiResult<MemberDetails> =
        apiCall {
            teamApi.memberDetails(teamId, userId).let { dto ->
                MemberDetails(
                    userId = dto.userId,
                    phone = dto.phone,
                    role = runCatching { MemberRole.valueOf(dto.role) }
                        .getOrDefault(MemberRole.MEMBER),
                    status = runCatching { MemberStatus.valueOf(dto.membershipStatus) }
                        .getOrDefault(MemberStatus.ACTIVE),
                    jerseyNumber = dto.jerseyNumber,
                    isTrainer = dto.isTrainer,
                    joinedAt = dto.joinedAt,
                    profile = dto.profile?.toDomain(),
                )
            }
        }

    override suspend fun changeRole(teamId: Long, userId: Long, role: MemberRole): ApiResult<Unit> =
        apiCall {
            teamApi.changeRole(teamId, userId, UpdateMemberRoleRequestDto(role.name))
            Unit
        }

    override suspend fun setJerseyNumber(teamId: Long, userId: Long, number: Int?): ApiResult<Unit> =
        apiCall {
            teamApi.setJersey(teamId, userId, SetJerseyNumberRequestDto(number))
            Unit
        }

    override suspend fun setTrainer(
        teamId: Long,
        userId: Long,
        isTrainer: Boolean,
    ): ApiResult<Unit> = apiCall {
        teamApi.setTrainer(teamId, userId, SetTrainerRequestDto(isTrainer))
        Unit
    }

    override suspend fun excludeMember(teamId: Long, userId: Long): ApiResult<Unit> = apiCall {
        teamApi.excludeMember(teamId, userId)
        Unit
    }

    override suspend fun restoreMember(teamId: Long, userId: Long): ApiResult<Unit> = apiCall {
        teamApi.restoreMember(teamId, userId)
        Unit
    }

    override suspend fun leave(teamId: Long): ApiResult<Unit> = apiCall {
        teamApi.leave(teamId)
        Unit
    }

    override suspend fun invite(teamId: Long, phone: String): ApiResult<Unit> = apiCall {
        teamApi.invite(teamId, InviteRequestDto(phone))
        Unit
    }

    override suspend fun outgoingInvitations(teamId: Long): ApiResult<List<Invitation>> = apiCall {
        teamApi.outgoingInvitations(teamId).map { it.toDomain() }
    }

    override suspend fun incomingInvitations(): ApiResult<List<Invitation>> = apiCall {
        invitationApi.myIncoming().map { it.toDomain() }
    }

    override suspend fun acceptInvitation(invitationId: Long): ApiResult<Unit> = apiCall {
        invitationApi.accept(invitationId)
        Unit
    }

    override suspend fun declineInvitation(invitationId: Long): ApiResult<Unit> = apiCall {
        invitationApi.decline(invitationId)
        Unit
    }

    override suspend fun cancelInvitation(invitationId: Long): ApiResult<Unit> = apiCall {
        invitationApi.cancel(invitationId)
        Unit
    }
}

private fun TeamDetailDto.toDomain() = TeamDetail(
    id = id,
    name = name,
    cityId = cityId,
    cityName = cityName,
    homeRegionId = homeRegionId,
    regionName = homeRegionName,
    logoUrl = logoUrl,
    description = description,
    season = season,
    status = status,
    myRole = myRole?.let { runCatching { MemberRole.valueOf(it) }.getOrNull() },
    membersCount = membersCount,
)

private fun info.prorabka.abik.android.data.dto.TeamDto.toDomain() = Team(
    id = id, name = name, cityName = cityName, regionName = homeRegionName,
    logoUrl = logoUrl, status = status,
)

private fun info.prorabka.abik.android.data.dto.TeamMemberDto.toDomain() = TeamMember(
    id = id,
    userId = userId,
    phone = phone ?: "",
    lastName = lastName ?: "",
    firstName = firstName ?: "",
    role = runCatching { MemberRole.valueOf(role) }.getOrDefault(MemberRole.MEMBER),
    status = runCatching { MemberStatus.valueOf(status) }.getOrDefault(MemberStatus.ACTIVE),
    jerseyNumber = jerseyNumber,
    isTrainer = isTrainer,
    positions = positions.mapNotNull { p -> runCatching { Position.valueOf(p) }.getOrNull() }.toSet(),
    avatarUrl = avatarUrl,
)

private fun TeamCreateData.toDto() = TeamCreateRequestDto(
    name = name,
    cityId = cityId,
    homeRegionId = homeRegionId,
    logoUrl = logoUrl,
    description = description,
    season = season,
)

private fun info.prorabka.abik.android.data.dto.InvitationDto.toDomain() = Invitation(
    id = id,
    teamId = teamId,
    teamName = teamName ?: "",
    invitedPhone = invitedPhone ?: "",
    status = status,
    expiresAt = expiresAt,
)
