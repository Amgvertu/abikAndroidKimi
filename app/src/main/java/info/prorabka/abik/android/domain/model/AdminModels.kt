package info.prorabka.abik.android.domain.model

data class AdminOverview(
    val totalUsers: Long,
    val totalTeams: Long,
    val chargeCount: Long,
    val chargeAmountKopecks: Long,
    val cancelledChargeCount: Long,
    val cancelledChargeAmountKopecks: Long,
    val paymentCount: Long,
    val paymentAmountKopecks: Long,
    val daily: List<DailyStat>,
)

data class AdminUserRow(
    val id: Long,
    val phone: String,
    val status: String,
    val superAdmin: Boolean,
    val adAdmin: Boolean,
    val fullName: String?,
    val cityId: Long?,
    val cityName: String?,
    val regionId: Long?,
    val regionName: String?,
    val teams: List<String>,
    val positions: List<String>,
    val createdAt: String?,
    val blockReason: String?,
)

data class AdminTeamFinance(
    val teamId: Long, val teamName: String, val role: String, val balanceKopecks: Long,
    val chargeCount: Long, val chargeAmountKopecks: Long,
    val paymentCount: Long, val paymentAmountKopecks: Long,
)
data class AdminUserDetails(val user: AdminUserRow, val teams: List<AdminTeamFinance>)

data class AdminTeamRow(
    val id: Long, val name: String, val status: String,
    val cityId: Long?, val cityName: String?, val regionId: Long?, val regionName: String?,
    val createdAt: String?, val blockReason: String?, val membersCount: Long,
    val eventsCount: Long, val chargeAmountKopecks: Long, val paymentAmountKopecks: Long,
)
data class AdminMemberRow(
    val userId: Long, val fullName: String, val phone: String, val role: String,
    val trainer: Boolean, val positions: List<String>, val balanceKopecks: Long,
)
data class AdminEventRow(
    val id: Long, val title: String, val section: String, val date: String,
    val time: String, val place: String?, val status: String,
)
data class AdminTeamDetails(val team: AdminTeamRow, val members: List<AdminMemberRow>, val events: List<AdminEventRow>)

data class AdminMessageRow(
    val id: Long, val title: String, val body: String, val linkUrl: String?,
    val audience: String, val recipients: Long, val read: Long, val createdAt: String?, val author: String,
)
data class AdminRegistrationEvent(
    val id: Long, val requestId: String?, val phoneMasked: String?, val stage: String,
    val status: String, val code: String?, val details: String?, val client: String?, val occurredAt: String?,
)

data class Advertisement(
    val id: Long, val title: String?, val body: String?, val imageUrl: String?,
    val linkUrl: String?, val buttonText: String?, val targetType: String, val targetId: Long?,
    val targetName: String?, val startsAt: String, val endsAt: String, val active: Boolean,
    val creatorUserId: Long, val creatorName: String?, val createdAt: String?,
)
data class AdvertisementDraft(
    val id: Long? = null,
    val title: String = "", val body: String = "", val imageUrl: String? = null,
    val linkUrl: String = "", val buttonText: String = "",
    val targetType: String = "GLOBAL", val targetId: Long? = null,
    val startsAt: String = "", val endsAt: String = "", val active: Boolean = true,
)
data class AdTotals(val views: Long, val viewers: Long, val clicks: Long, val uniqueClicks: Long)
data class AdGeoRow(
    val regionId: Long?, val regionName: String?, val cityId: Long?, val cityName: String?,
    val teamId: Long?, val teamName: String?, val views: Long, val viewers: Long,
    val clicks: Long, val uniqueClicks: Long,
)
data class AdStats(val adId: Long, val totals: AdTotals, val geography: List<AdGeoRow>)
data class AdminTarget(val id: Long, val name: String, val regionId: Long?, val cityId: Long?)

data class AdminMessageDraft(
    val title: String = "", val body: String = "", val linkUrl: String = "",
    val regionId: Long? = null, val cityId: Long? = null, val teamId: Long? = null,
    val position: String? = null,
)
