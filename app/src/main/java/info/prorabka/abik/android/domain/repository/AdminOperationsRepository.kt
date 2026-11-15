package info.prorabka.abik.android.domain.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.*

interface AdminOperationsRepository {
    suspend fun overview(from: String?, to: String?, regionId: Long?, cityId: Long?, teamId: Long?): ApiResult<AdminOverview>
    suspend fun users(): ApiResult<List<AdminUserRow>>
    suspend fun userDetails(id: Long): ApiResult<AdminUserDetails>
    suspend fun blockUser(id: Long, reason: String): ApiResult<Unit>
    suspend fun unblockUser(id: Long): ApiResult<Unit>
    suspend fun setAdAdmin(id: Long, value: Boolean): ApiResult<Unit>
    suspend fun teams(): ApiResult<List<AdminTeamRow>>
    suspend fun teamDetails(id: Long): ApiResult<AdminTeamDetails>
    suspend fun blockTeam(id: Long, reason: String): ApiResult<Unit>
    suspend fun unblockTeam(id: Long): ApiResult<Unit>
    suspend fun sendMessage(draft: AdminMessageDraft): ApiResult<Unit>
    suspend fun recipientCount(draft: AdminMessageDraft): ApiResult<Long>
    suspend fun messages(): ApiResult<List<AdminMessageRow>>
    suspend fun registrationEvents(status: String? = null): ApiResult<List<AdminRegistrationEvent>>
    suspend fun ads(): ApiResult<List<Advertisement>>
    suspend fun targetTeams(): ApiResult<List<AdminTarget>>
    suspend fun saveAd(draft: AdvertisementDraft): ApiResult<Unit>
    suspend fun setAdActive(id: Long, active: Boolean): ApiResult<Unit>
    suspend fun deleteAd(id: Long): ApiResult<Unit>
    suspend fun adStats(id: Long): ApiResult<AdStats>
    suspend fun activeAd(teamId: Long?): ApiResult<Advertisement?>
    suspend fun recordAdEvent(id: Long, type: String, sessionId: String, teamId: Long?): ApiResult<Unit>
}
