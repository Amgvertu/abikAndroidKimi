package info.prorabka.abik.android.data.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.AdminOperationsApi
import info.prorabka.abik.android.data.dto.*
import info.prorabka.abik.android.domain.model.*
import info.prorabka.abik.android.domain.repository.AdminOperationsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminOperationsRepositoryImpl @Inject constructor(
    private val api: AdminOperationsApi,
) : AdminOperationsRepository {
    override suspend fun overview(from: String?, to: String?, regionId: Long?, cityId: Long?, teamId: Long?) = apiCall {
        api.overview(from, to, regionId, cityId, teamId).let {
            AdminOverview(it.totalUsers, it.totalTeams, it.chargeCount, it.chargeAmountKopecks,
                it.cancelledChargeCount, it.cancelledChargeAmountKopecks, it.paymentCount,
                it.paymentAmountKopecks, it.daily.map { d -> DailyStat(d.date, d.users, d.teams) })
        }
    }
    override suspend fun users() = apiCall { api.users().map { it.toDomain() } }
    override suspend fun userDetails(id: Long) = apiCall {
        api.userDetails(id).let { d -> AdminUserDetails(d.user.toDomain(), d.teams.map {
            AdminTeamFinance(it.teamId, it.teamName, it.role, it.balanceKopecks, it.chargeCount,
                it.chargeAmountKopecks, it.paymentCount, it.paymentAmountKopecks)
        }) }
    }
    override suspend fun blockUser(id: Long, reason: String): ApiResult<Unit> = apiCall { api.blockUser(id, AdminBlockRequestDto(reason)); Unit }
    override suspend fun unblockUser(id: Long): ApiResult<Unit> = apiCall { api.unblockUser(id); Unit }
    override suspend fun setAdAdmin(id: Long, value: Boolean): ApiResult<Unit> = apiCall { api.setAdAdmin(id, AdminBooleanRequestDto(value)); Unit }
    override suspend fun teams() = apiCall { api.teams().map { it.toDomain() } }
    override suspend fun teamDetails(id: Long) = apiCall {
        api.teamDetails(id).let { d -> AdminTeamDetails(d.team.toDomain(), d.members.map {
            AdminMemberRow(it.userId, it.fullName, it.phone, it.role, it.trainer, it.positions, it.balanceKopecks)
        }, d.events.map { AdminEventRow(it.id, it.title, it.section, it.date, it.time, it.place, it.status) }) }
    }
    override suspend fun blockTeam(id: Long, reason: String): ApiResult<Unit> = apiCall { api.blockTeam(id, AdminBlockRequestDto(reason)); Unit }
    override suspend fun unblockTeam(id: Long): ApiResult<Unit> = apiCall { api.unblockTeam(id); Unit }
    override suspend fun sendMessage(draft: AdminMessageDraft): ApiResult<Unit> = apiCall { api.sendMessage(draft.toRequest()); Unit }
    override suspend fun recipientCount(draft: AdminMessageDraft) = apiCall { api.recipientCount(draft.toRequest()).count }
    override suspend fun messages() = apiCall { api.messages().map {
        AdminMessageRow(it.id, it.title, it.body, it.linkUrl, it.audience, it.recipients, it.read, it.createdAt, it.author)
    } }
    override suspend fun registrationEvents(status: String?) = apiCall { api.registrationEvents(status).map {
        AdminRegistrationEvent(it.id, it.requestId, it.phoneMasked, it.stage, it.status, it.code, it.details, it.client, it.occurredAt)
    } }
    override suspend fun ads() = apiCall { api.ads().map { it.toDomain() } }
    override suspend fun targetTeams() = apiCall { api.targetTeams().map { AdminTarget(it.id, it.name, it.regionId, it.cityId) } }
    override suspend fun saveAd(draft: AdvertisementDraft): ApiResult<Unit> = apiCall {
        if (draft.id == null) api.createAd(draft.toRequest()) else api.updateAd(draft.id, draft.toRequest())
        Unit
    }
    override suspend fun setAdActive(id: Long, active: Boolean): ApiResult<Unit> = apiCall { api.setAdActive(id, AdminBooleanRequestDto(active)); Unit }
    override suspend fun deleteAd(id: Long): ApiResult<Unit> = apiCall {
        val response = api.deleteAd(id)
        if (!response.isSuccessful) throw retrofit2.HttpException(response)
        Unit
    }
    override suspend fun adStats(id: Long) = apiCall { api.adStats(id).let {
        AdStats(it.adId, AdTotals(it.totals.views, it.totals.viewers, it.totals.clicks, it.totals.uniqueClicks), it.geography.map { g ->
            AdGeoRow(g.regionId, g.regionName, g.cityId, g.cityName, g.teamId, g.teamName, g.views, g.viewers, g.clicks, g.uniqueClicks)
        })
    } }
    override suspend fun activeAd(teamId: Long?) = apiCall {
        val response = api.activeAd(teamId)
        if (response.isSuccessful) response.body()?.toDomain() else throw retrofit2.HttpException(response)
    }
    override suspend fun recordAdEvent(id: Long, type: String, sessionId: String, teamId: Long?): ApiResult<Unit> = apiCall {
        val response = api.recordAdEvent(id, AdvertisementEventRequestDto(type, sessionId, teamId))
        if (!response.isSuccessful) throw retrofit2.HttpException(response)
        Unit
    }
}

private fun AdminUserRowDto.toDomain() = AdminUserRow(id, phone, status, superAdmin, adAdmin, fullName,
    cityId, cityName, regionId, regionName, teams, positions, createdAt, blockReason)
private fun AdminTeamRowDto.toDomain() = AdminTeamRow(id, name, status, cityId, cityName, regionId, regionName,
    createdAt, blockReason, membersCount, eventsCount, chargeAmountKopecks, paymentAmountKopecks)
private fun AdvertisementDto.toDomain() = Advertisement(id, title, body, imageUrl, linkUrl, buttonText,
    targetType, targetId, targetName, startsAt, endsAt, active, creatorUserId, creatorName, createdAt)
private fun AdminMessageDraft.toRequest() = AdminMessageRequestDto(title.trim(), body.trim(), linkUrl.trim().ifBlank { null },
    regionId, cityId, teamId, position)
private fun AdvertisementDraft.toRequest() = AdvertisementRequestDto(title.trim().ifBlank { null }, body.trim().ifBlank { null },
    imageUrl, linkUrl.trim().ifBlank { null }, buttonText.trim().ifBlank { null }, targetType, targetId,
    startsAt, endsAt, active)
