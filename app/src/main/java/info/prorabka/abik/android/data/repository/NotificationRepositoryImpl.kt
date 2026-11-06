package info.prorabka.abik.android.data.repository

import android.util.Log
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.DeviceApi
import info.prorabka.abik.android.data.api.NotificationApi
import info.prorabka.abik.android.data.dto.DeviceRegisterRequest
import info.prorabka.abik.android.data.dto.NotificationDto
import info.prorabka.abik.android.data.dto.NotificationSettingsDto
import info.prorabka.abik.android.domain.model.AppNotification
import info.prorabka.abik.android.domain.model.NotificationSettings
import info.prorabka.abik.android.domain.model.NotificationType
import info.prorabka.abik.android.domain.repository.DeviceRepository
import info.prorabka.abik.android.domain.repository.NotificationRepository
import info.prorabka.abik.android.fcm.DeviceTokenProvider
import info.prorabka.abik.android.push.HuaweiTokenProvider
import info.prorabka.abik.android.push.PushProviders
import info.prorabka.abik.android.push.RuStoreTokenProvider
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val api: NotificationApi,
) : NotificationRepository {

    override suspend fun list(onlyUnread: Boolean): ApiResult<List<AppNotification>> = apiCall {
        api.list(onlyUnread).map { it.toDomain() }
    }

    override suspend fun unreadCount(): ApiResult<Int> = apiCall {
        api.unreadCount().count.toInt()
    }

    override suspend fun markRead(id: Long): ApiResult<Unit> = apiCall {
        api.markRead(id)
        Unit
    }

    override suspend fun markAllRead(): ApiResult<Unit> = apiCall {
        api.markAllRead()
        Unit
    }

    override suspend fun settings(): ApiResult<NotificationSettings> = apiCall {
        api.settings().toDomain()
    }

    override suspend fun saveSettings(settings: NotificationSettings): ApiResult<NotificationSettings> = apiCall {
        api.saveSettings(
            NotificationSettingsDto(
                remindersEnabled = settings.remindersEnabled,
                reminderCount = settings.reminderCount,
                leadTimeMinutes = settings.leadTimeMinutes,
                intervalMinutes = settings.intervalMinutes,
            ),
        ).toDomain()
    }

    private fun NotificationSettingsDto.toDomain() = NotificationSettings(
        remindersEnabled = remindersEnabled,
        reminderCount = reminderCount,
        leadTimeMinutes = leadTimeMinutes,
        intervalMinutes = intervalMinutes,
    )

    private fun NotificationDto.toDomain() = AppNotification(
        id = id,
        type = runCatching { NotificationType.valueOf(type) }
            .getOrDefault(NotificationType.UNKNOWN),
        title = title,
        body = body,
        teamId = teamId,
        payload = payload?.mapValues { (_, v) -> v.asDisplayString() },
        isRead = isRead,
        sentAtIso = sentAt,
        readAtIso = readAt,
    )

    private fun JsonElement.asDisplayString(): String =
        runCatching {
            if (jsonPrimitive.isString) jsonPrimitive.content else toString()
        }.getOrDefault(toString())
}

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val api: DeviceApi,
    private val tokenProvider: DeviceTokenProvider,
    private val ruStoreTokenProvider: RuStoreTokenProvider,
    private val huaweiTokenProvider: HuaweiTokenProvider,
) : DeviceRepository {

    /** Регистрируем токены всех доступных провайдеров: сервер перебирает их при отправке. */
    override suspend fun registerCurrentDevice() {
        registerToken(PushProviders.FCM) { tokenProvider.currentToken() }
        registerToken(PushProviders.RUSTORE) { ruStoreTokenProvider.currentToken() }
        registerToken(PushProviders.HUAWEI) { huaweiTokenProvider.currentToken() }
    }

    private suspend fun registerToken(provider: String, getToken: suspend () -> String?) {
        val token = runCatching { getToken() }.getOrNull() ?: return
        runCatching {
            api.register(
                DeviceRegisterRequest(token = token, platform = PLATFORM, provider = provider),
            )
        }.onFailure { Log.w(TAG, "Не удалось зарегистрировать устройство ($provider)", it) }
    }

    override suspend fun unregisterCurrentDevice() {
        listOf(
            tokenProvider.savedToken,
            ruStoreTokenProvider.savedToken,
            huaweiTokenProvider.savedToken,
        ).filterNotNull().forEach { token ->
            runCatching { api.unregister(token) }
                .onFailure { Log.w(TAG, "Не удалось отозвать устройство", it) }
        }
    }

    private companion object {
        const val PLATFORM = "ANDROID"
        const val TAG = "DeviceRepository"
    }
}
