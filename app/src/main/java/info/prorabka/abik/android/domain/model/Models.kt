package info.prorabka.abik.android.domain.model

import java.time.LocalDate

enum class StickHand(val display: String) {
    LEFT("Левый"),
    RIGHT("Правый"),
}

enum class Position(val display: String) {
    GOALKEEPER("Вратарь"),
    DEFENDER("Защитник"),
    FORWARD("Нападающий"),
}

data class User(
    val id: Long,
    val phone: String,
    val status: String,
    val isSuperAdmin: Boolean = false,
)

data class Profile(
    val userId: Long,
    val cityId: Long,
    val cityName: String,
    val regionId: Long?,
    val regionName: String?,
    val lastName: String,
    val firstName: String,
    val middleName: String,
    val birthDate: LocalDate?,
    val heightCm: Int,
    val weightKg: Int,
    val stickHand: StickHand,
    val positions: Set<Position>,
    val jerseySize: String?,
    val coversSize: String?,
    val gaitersSize: String?,
    val avatarUrl: String?,
) {
    val fullName: String get() = "$lastName $firstName $middleName".trim()
    val shortName: String get() = "$lastName ${firstName.firstOrNull()?.plus(".") ?: ""}"
}

data class Region(val id: Long, val name: String)

data class City(val id: Long, val regionId: Long, val name: String)

data class Team(
    val id: Long,
    val name: String,
    val cityName: String?,
    val regionName: String?,
    val logoUrl: String?,
    val status: String,
)

enum class MemberRole(val display: String) {
    OWNER("Владелец"),
    MODERATOR("Модератор"),
    MEMBER("Участник"),
    ADMIN("Админ"),
}

enum class MemberStatus(val display: String) {
    ACTIVE("Активен"),
    EXCLUDED("Исключён"),
    LEFT("Вышел"),
}

data class TeamMember(
    val id: Long,
    val userId: Long,
    val phone: String,
    val lastName: String,
    val firstName: String,
    val role: MemberRole,
    val status: MemberStatus,
    val jerseyNumber: Int?,
) {
    val displayName: String get() = "$lastName $firstName".trim().ifBlank { phone }
}

data class TeamDetail(
    val id: Long,
    val name: String,
    val cityId: Long?,
    val cityName: String?,
    val homeRegionId: Long?,
    val regionName: String?,
    val logoUrl: String?,
    val description: String?,
    val season: String?,
    val status: String,
    val myRole: MemberRole?,
    val membersCount: Long,
)

data class Invitation(
    val id: Long,
    val teamId: Long,
    val teamName: String,
    val invitedPhone: String,
    val status: String,
    val expiresAt: String?,
)

data class TeamCreateData(
    val name: String,
    val cityId: Long,
    val homeRegionId: Long,
    val logoUrl: String,
    val description: String?,
    val season: String?,
)
