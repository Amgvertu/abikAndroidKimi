package info.prorabka.abik.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminUserDto(
    val id: Long,
    val phone: String,
    val status: String,
    val isSuperAdmin: Boolean = false,
    val createdAt: String? = null,
)

@Serializable
data class CreateRegionRequestDto(
    val name: String,
    val code: String,
    val timezone: String,
)

@Serializable
data class CreateCityRequestDto(
    val regionId: Long,
    val name: String,
)

@Serializable
data class AdminStatsDto(
    val totalUsers: Long,
    val totalTeams: Long,
    val usersToday: Long,
    val usersWeek: Long,
    val usersMonth: Long,
    val teamsToday: Long,
    val teamsWeek: Long,
    val teamsMonth: Long,
    val last30Days: List<DailyStatDto> = emptyList(),
)

@Serializable
data class DailyStatDto(val date: String, val users: Long, val teams: Long)

@Serializable
data class AppReleaseRequestDto(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val mandatory: Boolean,
)
