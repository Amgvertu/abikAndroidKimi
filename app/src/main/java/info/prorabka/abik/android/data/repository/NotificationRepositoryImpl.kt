package info.prorabka.abik.android.data.repository

import android.util.Log
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.DeviceApi
import info.prorabka.abik.android.data.api.NotificationApi
import info.prorabka.abik.android.data.dto.DeviceRegisterRequest
import info.prorabka.abik.android.data.dto.NotificationDto
import info.prorabka.abik.android.domain.model.AppNotification
import info.prorabka.abik.android.domain.model.NotificationType
import info.prorabka.abik.android.domain.repository.DeviceRepository
import info.prorabka.abik.android.domain.repository.NotificationRepository
import info.prorabka.abik.android.fcm.DeviceTokenProvider
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
) : DeviceRepository {

    override suspend fun registerCurrentDevice() {
        val token = tokenProvider.currentToken() ?: return
        runCatching {
            api.register(DeviceRegisterRequest(token = token, platform = PLATFORM))
        }.onFailure { Log.w(TAG, "Не удалось зарегистрировать устройство", it) }
    }

    override suspend fun unregisterCurrentDevice() {
        val token = tokenProvider.savedToken ?: return
        runCatching { api.unregister(token) }
            .onFailure { Log.w(TAG, "Не удалось отозвать устройство", it) }
    }

    private companion object {
        const val PLATFORM = "ANDROID"
        const val TAG = "DeviceRepository"
    }
}
