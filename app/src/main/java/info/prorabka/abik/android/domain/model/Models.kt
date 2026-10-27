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
    val isTrainer: Boolean = false,
) {
    val displayName: String get() = "$lastName $firstName".trim().ifBlank { phone }
}

/** BE-07: карточка участника команды — членство + профиль. */
data class MemberDetails(
    val userId: Long,
    val phone: String?,
    val role: MemberRole,
    val status: MemberStatus,
    val jerseyNumber: Int?,
    val isTrainer: Boolean,
    val joinedAt: String?,
    val profile: Profile?,
)

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

// --- Состав регулярки (AND-05) ---

data class Season(
    val name: String,
    val isCurrent: Boolean,
)

enum class RosterStatus(val display: String) {
    ACTIVE("Активен"),
    REMOVED("Исключён"),
}

data class RosterItem(
    val userId: Long,
    val fullName: String,
    val positions: Set<Position>,
    val status: RosterStatus,
    val addedAt: String?,
)

/** Параметры стартового взноса при добавлении в состав. */
data class EntryFeeData(
    val groupId: Long,
    val subgroupId: Long?,
    val amountKopecks: Long,
    val dueDateIso: String,
)

// --- События (AND-06/07) ---

enum class EventSection(val display: String) {
    TRAINING("Тренировки"),
    REGULAR("Регулярка"),
}

enum class EventType(val display: String) {
    TRAINING("Тренировка"),
    GAME("Игра"),
    MATCH("Матч"),
}

enum class VoteAnswer(val display: String) {
    YES("Иду"),
    NO("Не иду"),
    MAYBE("Под вопросом"),
}

enum class SweaterColor(val display: String) {
    WHITE("Белые"),
    BLACK("Чёрные"),
    RED("Красные"),
}

enum class LineupRole { GOALKEEPER, DEFENDER, FORWARD }

data class EventListItem(
    val id: Long,
    val title: String,
    val section: EventSection,
    val type: EventType,
    val dateIso: String,
    val time: String,
    val place: String,
    val myVote: VoteAnswer?,
    val votesYes: Long,
    val votesNo: Long,
    val votesMaybe: Long,
)

data class EventVoter(
    val userId: Long,
    val fullName: String,
    val answer: VoteAnswer?,
    val positions: Set<Position>,
    val isTrainer: Boolean,
    val sweaterColor: SweaterColor?,
)

/** Один слот заявки на игру. lineNumber = null для вратаря. */
data class LineupSlot(
    val lineNumber: Int?,
    val slotNumber: Int,
    val role: LineupRole,
    val userId: Long,
)

data class EventDetail(
    val id: Long,
    val title: String,
    val section: EventSection,
    val type: EventType,
    val dateIso: String,
    val time: String,
    val place: String,
    val note: String?,
    val status: String,
    val seriesId: Long?,
    val myVote: VoteAnswer?,
    val voters: List<EventVoter>,
    val lineup: List<LineupSlot>?,
)

/** Данные формы создания/редактирования события. weekdays/untilDateIso = null — разовое. */
data class EventFormData(
    val title: String,
    val section: EventSection,
    val type: EventType,
    val dateIso: String,
    val time: String,
    val place: String,
    val note: String?,
    val weekdays: Set<Int>?,
    val untilDateIso: String?,
)
