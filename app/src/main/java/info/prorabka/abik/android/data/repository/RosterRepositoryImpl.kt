package info.prorabka.abik.android.data.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.RosterApi
import info.prorabka.abik.android.data.dto.EntryFeeDto
import info.prorabka.abik.android.data.dto.RosterAddRequestDto
import info.prorabka.abik.android.data.dto.RosterReplaceRequestDto
import info.prorabka.abik.android.domain.model.EntryFeeData
import info.prorabka.abik.android.domain.model.Position
import info.prorabka.abik.android.domain.model.RosterItem
import info.prorabka.abik.android.domain.model.RosterStatus
import info.prorabka.abik.android.domain.model.Season
import info.prorabka.abik.android.domain.repository.RosterRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RosterRepositoryImpl @Inject constructor(
    private val api: RosterApi,
) : RosterRepository {

    override suspend fun seasons(teamId: Long): ApiResult<List<Season>> = apiCall {
        api.seasons(teamId).map { Season(name = it.season, isCurrent = it.current) }
    }

    override suspend fun roster(teamId: Long, season: String): ApiResult<List<RosterItem>> =
        apiCall {
            api.roster(teamId, season).map { dto ->
                RosterItem(
                    userId = dto.userId,
                    fullName = dto.fullName ?: "user#${dto.userId}",
                    positions = dto.positions.mapNotNull {
                        runCatching { Position.valueOf(it) }.getOrNull()
                    }.toSet(),
                    status = runCatching { RosterStatus.valueOf(dto.status) }
                        .getOrDefault(RosterStatus.ACTIVE),
                    addedAt = dto.addedAt,
                )
            }
        }

    override suspend fun replace(
        teamId: Long,
        season: String,
        userIds: Set<Long>,
    ): ApiResult<Unit> = apiCall {
        api.replace(teamId, season, RosterReplaceRequestDto(userIds))
        Unit
    }

    override suspend fun addMember(
        teamId: Long,
        season: String,
        userId: Long,
        entryFee: EntryFeeData?,
    ): ApiResult<Unit> = apiCall {
        api.add(
            teamId, season,
            RosterAddRequestDto(
                userId = userId,
                chargeEntryFee = entryFee != null,
                entryFee = entryFee?.let {
                    EntryFeeDto(
                        groupId = it.groupId,
                        subgroupId = it.subgroupId,
                        amountKopecks = it.amountKopecks,
                        dueDate = it.dueDateIso,
                    )
                },
            ),
        )
        Unit
    }

    override suspend fun removeMember(teamId: Long, season: String, userId: Long): ApiResult<Unit> =
        apiCall {
            api.remove(teamId, userId, season)
            Unit
        }
}
