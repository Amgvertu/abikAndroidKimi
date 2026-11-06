package info.prorabka.abik.android.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class NotificationDto(
    val id: Long,
    val type: String,
    val title: String,
    val body: String,
    val teamId: Long? = null,
    val payload: Map<String, JsonElement>? = null,
    val isRead: Boolean = false,
    val sentAt: String,
    val readAt: String? = null,
)

@Serializable
data class UnreadCountResponse(val count: Long)

@Serializable
data class NotificationSettingsDto(
    val remindersEnabled: Boolean = true,
    val reminderCount: Int = 1,
    val leadTimeMinutes: Int = 1440,
    val intervalMinutes: Int = 60,
)

@Serializable
data class DeviceRegisterRequest(
    val token: String,
    val platform: String,
    /** Провайдер push: FCM, RUSTORE, HUAWEI. */
    val provider: String = "FCM",
)
