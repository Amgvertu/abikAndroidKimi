package info.prorabka.abik.android.domain.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.City
import info.prorabka.abik.android.domain.model.EntryFeeData
import info.prorabka.abik.android.domain.model.EventDetail
import info.prorabka.abik.android.domain.model.EventFormData
import info.prorabka.abik.android.domain.model.EventListItem
import info.prorabka.abik.android.domain.model.Invitation
import info.prorabka.abik.android.domain.model.LineupSlot
import info.prorabka.abik.android.domain.model.MemberDetails
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.Position
import info.prorabka.abik.android.domain.model.Profile
import info.prorabka.abik.android.domain.model.ProfileFormData
import info.prorabka.abik.android.domain.model.Region
import info.prorabka.abik.android.domain.model.RosterItem
import info.prorabka.abik.android.domain.model.Season
import info.prorabka.abik.android.domain.model.StickHand
import info.prorabka.abik.android.domain.model.SweaterColor
import info.prorabka.abik.android.domain.model.Team
import info.prorabka.abik.android.domain.model.TeamCreateData
import info.prorabka.abik.android.domain.model.TeamDetail
import info.prorabka.abik.android.domain.model.TeamMember
import info.prorabka.abik.android.domain.model.User
import info.prorabka.abik.android.domain.model.VoteAnswer

interface AuthRepository {
    suspend fun registerStart(phone: String, password: String, acceptedPd: Boolean): ApiResult<Unit>
    suspend fun registerConfirm(phone: String, code: String): ApiResult<Unit>
    suspend fun login(phone: String, password: String): ApiResult<Unit>
    suspend fun logout(): ApiResult<Unit>
    suspend fun passwordResetStart(phone: String): ApiResult<Unit>
    suspend fun passwordResetConfirm(phone: String, code: String, newPassword: String): ApiResult<Unit>
}

interface UserRepository {
    suspend fun me(): ApiResult<User>
    suspend fun myProfile(): ApiResult<Profile>
    suspend fun profileOf(userId: Long): ApiResult<Profile>
    suspend fun updateMyProfile(
        cityId: Long,
        lastName: String,
        firstName: String,
        middleName: String,
        birthDateIso: String,
        heightCm: Int,
        weightKg: Int,
        stickHand: StickHand,
        positions: Set<Position>,
        jerseySize: String?,
        coversSize: String?,
        gaitersSize: String?,
        avatarUrl: String?,
    ): ApiResult<Profile>

    suspend fun phoneChangeStart(): ApiResult<Unit>
    suspend fun phoneChangeVerifyOld(code: String): ApiResult<String>
    suspend fun phoneChangeSendNew(changeToken: String, newPhone: String): ApiResult<Unit>
    suspend fun phoneChangeConfirm(changeToken: String, code: String): ApiResult<Unit>
}

interface GeoRepository {
    suspend fun regions(): ApiResult<List<Region>>
    suspend fun citiesByRegion(regionId: Long): ApiResult<List<City>>
    suspend fun searchCities(query: String): ApiResult<List<City>>
}

interface TeamRepository {
    suspend fun myTeams(): ApiResult<List<Team>>
    suspend fun detail(teamId: Long): ApiResult<TeamDetail>
    suspend fun create(data: TeamCreateData): ApiResult<TeamDetail>
    suspend fun update(teamId: Long, data: TeamCreateData): ApiResult<TeamDetail>
    suspend fun archive(teamId: Long): ApiResult<Unit>
    suspend fun transferOwnership(teamId: Long, newOwnerUserId: Long): ApiResult<Unit>
    suspend fun members(teamId: Long): ApiResult<List<TeamMember>>
    suspend fun memberDetails(teamId: Long, userId: Long): ApiResult<MemberDetails>
    suspend fun updateMemberProfile(teamId: Long, userId: Long, data: ProfileFormData): ApiResult<Unit>
    suspend fun changeRole(teamId: Long, userId: Long, role: MemberRole): ApiResult<Unit>
    suspend fun setJerseyNumber(teamId: Long, userId: Long, number: Int?): ApiResult<Unit>
    suspend fun setTrainer(teamId: Long, userId: Long, isTrainer: Boolean): ApiResult<Unit>
    suspend fun excludeMember(teamId: Long, userId: Long): ApiResult<Unit>
    suspend fun restoreMember(teamId: Long, userId: Long): ApiResult<Unit>
    suspend fun leave(teamId: Long): ApiResult<Unit>
    suspend fun invite(teamId: Long, phone: String): ApiResult<Unit>
    suspend fun outgoingInvitations(teamId: Long): ApiResult<List<Invitation>>
    suspend fun incomingInvitations(): ApiResult<List<Invitation>>
    suspend fun acceptInvitation(invitationId: Long): ApiResult<Unit>
    suspend fun declineInvitation(invitationId: Long): ApiResult<Unit>
    suspend fun cancelInvitation(invitationId: Long): ApiResult<Unit>
}

interface RosterRepository {
    suspend fun seasons(teamId: Long): ApiResult<List<Season>>
    suspend fun roster(teamId: Long, season: String): ApiResult<List<RosterItem>>
    suspend fun replace(teamId: Long, season: String, userIds: Set<Long>): ApiResult<Unit>
    suspend fun addMember(
        teamId: Long,
        season: String,
        userId: Long,
        entryFee: EntryFeeData?,
    ): ApiResult<Unit>
    suspend fun removeMember(teamId: Long, season: String, userId: Long): ApiResult<Unit>
}

interface EventRepository {
    suspend fun events(teamId: Long, fromIso: String, toIso: String): ApiResult<List<EventListItem>>
    suspend fun detail(teamId: Long, eventId: Long): ApiResult<EventDetail>
    suspend fun create(teamId: Long, data: EventFormData): ApiResult<EventDetail>
    suspend fun update(
        teamId: Long,
        eventId: Long,
        data: EventFormData,
        scope: String,
    ): ApiResult<EventDetail>
    suspend fun delete(teamId: Long, eventId: Long, scope: String): ApiResult<Unit>
    suspend fun vote(teamId: Long, eventId: Long, answer: VoteAnswer): ApiResult<Unit>
    suspend fun setSweaters(
        teamId: Long,
        eventId: Long,
        items: Map<Long, SweaterColor?>,
    ): ApiResult<Unit>
    suspend fun setLineup(teamId: Long, eventId: Long, slots: List<LineupSlot>): ApiResult<Unit>
}
