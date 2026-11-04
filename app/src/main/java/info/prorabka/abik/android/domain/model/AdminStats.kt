package info.prorabka.abik.android.domain.model

data class AdminStats(
    val totalUsers: Long,
    val totalTeams: Long,
    val usersToday: Long,
    val usersWeek: Long,
    val usersMonth: Long,
    val teamsToday: Long,
    val teamsWeek: Long,
    val teamsMonth: Long,
    val last30Days: List<DailyStat>,
)

data class DailyStat(val date: String, val users: Long, val teams: Long)
