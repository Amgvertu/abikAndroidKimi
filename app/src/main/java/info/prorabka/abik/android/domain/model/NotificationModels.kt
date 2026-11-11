package info.prorabka.abik.android.domain.model

/** Типы уведомлений сервера (NotificationType). */
enum class NotificationType {
    INVITATION,
    ROLE_CHANGED,
    NEW_CHARGE,
    PAYMENT,
    DEBT_REMINDER,
    OWNERSHIP_TRANSFER,
    EXCLUDED,
    ARCHIVED,
    EVENT_CREATED,
    EVENT_UPDATED,
    EVENT_CANCELLED,
    ROSTER_ADDED,
    ROSTER_REMOVED,
    TRAINER_ASSIGNED,
    EVENT_REMINDER,
    HOLIDAY_REMINDER,
    HOLIDAY_GREETING,
    UNKNOWN,
}

data class NotificationSettings(
    val remindersEnabled: Boolean = true,
    val reminderCount: Int = 1,
    val leadTimeMinutes: Int = 1440,
    val intervalMinutes: Int = 60,
)

data class AppNotification(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val body: String,
    val teamId: Long?,
    /** Доп. данные (eventId, teamId и т.п.) — строковое представление значений. */
    val payload: Map<String, String>?,
    val isRead: Boolean,
    val sentAtIso: String,
    val readAtIso: String?,
)
