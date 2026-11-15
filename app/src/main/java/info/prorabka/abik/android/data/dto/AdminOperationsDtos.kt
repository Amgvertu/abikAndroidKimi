package info.prorabka.abik.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminOverviewDto(
    val totalUsers: Long, val totalTeams: Long,
    val chargeCount: Long, val chargeAmountKopecks: Long,
    val cancelledChargeCount: Long, val cancelledChargeAmountKopecks: Long,
    val paymentCount: Long, val paymentAmountKopecks: Long,
    val daily: List<AdminDailyDto> = emptyList(),
)
@Serializable data class AdminDailyDto(val date: String, val users: Long, val teams: Long)

@Serializable
data class AdminUserRowDto(
    val id: Long, val phone: String, val status: String,
    val superAdmin: Boolean = false, val adAdmin: Boolean = false,
    val fullName: String? = null, val cityId: Long? = null, val cityName: String? = null,
    val regionId: Long? = null, val regionName: String? = null,
    val teams: List<String> = emptyList(), val positions: List<String> = emptyList(),
    val createdAt: String? = null, val blockReason: String? = null,
)
@Serializable
data class AdminTeamFinanceDto(
    val teamId: Long, val teamName: String, val role: String, val balanceKopecks: Long,
    val chargeCount: Long, val chargeAmountKopecks: Long,
    val paymentCount: Long, val paymentAmountKopecks: Long,
)
@Serializable data class AdminUserDetailsDto(val user: AdminUserRowDto, val teams: List<AdminTeamFinanceDto>)

@Serializable
data class AdminTeamRowDto(
    val id: Long, val name: String, val status: String,
    val cityId: Long? = null, val cityName: String? = null,
    val regionId: Long? = null, val regionName: String? = null,
    val createdAt: String? = null, val blockReason: String? = null,
    val membersCount: Long = 0, val eventsCount: Long = 0,
    val chargeAmountKopecks: Long = 0, val paymentAmountKopecks: Long = 0,
)
@Serializable
data class AdminMemberRowDto(
    val userId: Long, val fullName: String, val phone: String, val role: String,
    val trainer: Boolean = false, val positions: List<String> = emptyList(), val balanceKopecks: Long = 0,
)
@Serializable
data class AdminEventRowDto(
    val id: Long, val title: String, val section: String, val date: String,
    val time: String, val place: String? = null, val status: String,
)
@Serializable data class AdminTeamDetailsDto(val team: AdminTeamRowDto, val members: List<AdminMemberRowDto>, val events: List<AdminEventRowDto>)

@Serializable
data class AdminMessageRequestDto(
    val title: String, val body: String, val linkUrl: String? = null,
    val regionId: Long? = null, val cityId: Long? = null, val teamId: Long? = null,
    val position: String? = null,
)
@Serializable
data class AdminMessageRowDto(
    val id: Long, val title: String, val body: String, val linkUrl: String? = null,
    val audience: String, val recipients: Long, val read: Long,
    val createdAt: String? = null, val author: String,
)
@Serializable data class AdminRecipientCountDto(val count: Long)

@Serializable
data class AdminRegistrationEventDto(
    val id: Long, val requestId: String? = null, val phoneMasked: String? = null,
    val stage: String, val status: String, val code: String? = null,
    val details: String? = null, val client: String? = null, val occurredAt: String? = null,
)

@Serializable
data class AdvertisementDto(
    val id: Long, val title: String? = null, val body: String? = null, val imageUrl: String? = null,
    val linkUrl: String? = null, val buttonText: String? = null,
    val targetType: String, val targetId: Long? = null, val targetName: String? = null,
    val startsAt: String, val endsAt: String, val active: Boolean,
    val creatorUserId: Long, val creatorName: String? = null, val createdAt: String? = null,
)
@Serializable
data class AdvertisementRequestDto(
    val title: String? = null, val body: String? = null, val imageUrl: String? = null,
    val linkUrl: String? = null, val buttonText: String? = null,
    val targetType: String, val targetId: Long? = null,
    val startsAt: String, val endsAt: String, val active: Boolean,
)
@Serializable data class AdvertisementEventRequestDto(val type: String, val sessionId: String, val teamId: Long? = null)
@Serializable data class AdTotalsDto(val views: Long, val viewers: Long, val clicks: Long, val uniqueClicks: Long)
@Serializable
data class AdGeoRowDto(
    val regionId: Long? = null, val regionName: String? = null, val cityId: Long? = null,
    val cityName: String? = null, val teamId: Long? = null, val teamName: String? = null,
    val views: Long, val viewers: Long, val clicks: Long, val uniqueClicks: Long,
)
@Serializable data class AdStatsDto(val adId: Long, val totals: AdTotalsDto, val geography: List<AdGeoRowDto>)
@Serializable data class AdminBlockRequestDto(val reason: String)
@Serializable data class AdminBooleanRequestDto(val value: Boolean)
@Serializable data class AdminTargetDto(val id: Long, val name: String, val regionId: Long? = null, val cityId: Long? = null)
